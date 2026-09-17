import json
from uuid import uuid4

from generatorgena_ml.controller import RabbitGenerationCommandController
from generatorgena_ml.exception import EventPublicationError
from generatorgena_ml.mapper import GenerationMessageMapper


class FakeMessage:
    def __init__(self, body: bytes):
        self.body = body
        self.acked = False
        self.rejected = False
        self.nacked = False
        self.ack_error: Exception | None = None

    @property
    def processed(self) -> bool:
        return self.acked or self.rejected or self.nacked

    async def ack(self) -> None:
        if self.ack_error:
            raise self.ack_error
        self.acked = True

    async def reject(self, requeue: bool) -> None:
        assert requeue is False
        self.rejected = True

    async def nack(self, requeue: bool) -> None:
        assert requeue is True
        self.nacked = True


class FakeService:
    def __init__(self, error: Exception | None = None):
        self.error = error
        self.commands: list[object] = []

    async def generate(self, command: object) -> None:
        if self.error:
            raise self.error
        self.commands.append(command)


def command_message() -> FakeMessage:
    return FakeMessage(
        json.dumps(
            {
                "commandId": str(uuid4()),
                "generationId": str(uuid4()),
                "prompt": "flowers",
                "status": "QUEUED",
            }
        ).encode()
    )


async def test_valid_command_is_handled_and_acked() -> None:
    message = command_message()
    service = FakeService()
    controller = RabbitGenerationCommandController(
        service,  # type: ignore[arg-type]
        GenerationMessageMapper(),
    )

    await controller.handle(message)  # type: ignore[arg-type]

    assert message.acked is True
    assert len(service.commands) == 1


async def test_malformed_message_is_rejected() -> None:
    message = FakeMessage(b"not-json")
    controller = RabbitGenerationCommandController(
        FakeService(),  # type: ignore[arg-type]
        GenerationMessageMapper(),
    )

    await controller.handle(message)  # type: ignore[arg-type]

    assert message.rejected is True


async def test_publication_failure_requeues_message() -> None:
    message = command_message()
    controller = RabbitGenerationCommandController(
        FakeService(EventPublicationError("down")),  # type: ignore[arg-type]
        GenerationMessageMapper(),
    )

    await controller.handle(message)  # type: ignore[arg-type]

    assert message.acked is False
    assert message.nacked is True


async def test_ack_failure_requeues_message() -> None:
    message = command_message()
    message.ack_error = OSError("channel closed")
    controller = RabbitGenerationCommandController(
        FakeService(),  # type: ignore[arg-type]
        GenerationMessageMapper(),
    )

    await controller.handle(message)  # type: ignore[arg-type]

    assert message.acked is False
    assert message.nacked is True
