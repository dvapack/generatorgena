import json
from dataclasses import dataclass, field
from uuid import uuid4

import pytest

from generatorgena_ml.domain.contracts import CompletedEvent, FailedEvent, ProcessingEvent
from generatorgena_ml.domain.errors import MessagingError
from generatorgena_ml.generation.generator import GeneratedImage
from generatorgena_ml.storage.minio import StorageError
from generatorgena_ml.worker import GenerationWorker


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


class FakeGenerator:
    def __init__(self, error: Exception | None = None):
        self.error = error

    def generate_from_text(self, prompt: str) -> GeneratedImage:
        if self.error:
            raise self.error
        return GeneratedImage(b"png", "image/png", 3, 64, 64)


class FakeStorage:
    def __init__(self, error: Exception | None = None):
        self.error = error
        self.uploads: list[tuple[str, GeneratedImage]] = []

    async def put_image(self, object_key: str, image: GeneratedImage) -> None:
        if self.error:
            raise self.error
        self.uploads.append((object_key, image))


@dataclass
class FakePublisher:
    fail_on: str | None = None
    events: list[tuple[object, str]] = field(default_factory=list)

    async def publish_event(self, event: object, routing_key: str) -> None:
        if routing_key == self.fail_on:
            raise MessagingError("publish failed")
        self.events.append((event, routing_key))


def command_message(generation_type: str = "IMAGE") -> FakeMessage:
    return FakeMessage(
        json.dumps(
            {
                "commandId": str(uuid4()),
                "generationId": str(uuid4()),
                "prompt": "flowers",
                "type": generation_type,
            }
        ).encode()
    )


async def test_success_uploads_then_completes_and_acks() -> None:
    storage = FakeStorage()
    publisher = FakePublisher()
    message = command_message()
    worker = GenerationWorker(FakeGenerator(), storage, publisher)  # type: ignore[arg-type]

    await worker.handle(message)  # type: ignore[arg-type]

    assert message.acked is True
    assert len(storage.uploads) == 1
    assert [type(event) for event, _ in publisher.events] == [
        ProcessingEvent,
        CompletedEvent,
    ]
    assert publisher.events[1][1] == "generation.completed"


async def test_unsupported_type_publishes_failed_and_acks() -> None:
    publisher = FakePublisher()
    message = command_message("VIDEO")
    worker = GenerationWorker(FakeGenerator(), FakeStorage(), publisher)  # type: ignore[arg-type]

    await worker.handle(message)  # type: ignore[arg-type]

    failed = publisher.events[-1][0]
    assert message.acked is True
    assert isinstance(failed, FailedEvent)
    assert failed.error.code == "UNSUPPORTED_TYPE"
    assert failed.error.message == "Тип генерации VIDEO не поддерживается"


async def test_storage_error_publishes_failed_and_acks() -> None:
    publisher = FakePublisher()
    message = command_message()
    storage = FakeStorage(StorageError("down", retryable=True))
    worker = GenerationWorker(FakeGenerator(), storage, publisher)  # type: ignore[arg-type]

    await worker.handle(message)  # type: ignore[arg-type]

    failed = publisher.events[-1][0]
    assert message.acked is True
    assert isinstance(failed, FailedEvent)
    assert failed.error.code == "STORAGE_ERROR"
    assert failed.error.message == "Не удалось сохранить сгенерированное изображение"
    assert failed.error.retryable is True


async def test_generation_error_message_is_in_russian() -> None:
    publisher = FakePublisher()
    message = command_message()
    worker = GenerationWorker(  # type: ignore[arg-type]
        FakeGenerator(RuntimeError("model failed")),
        FakeStorage(),
        publisher,
    )

    await worker.handle(message)  # type: ignore[arg-type]

    failed = publisher.events[-1][0]
    assert message.acked is True
    assert isinstance(failed, FailedEvent)
    assert failed.error.code == "GENERATION_ERROR"
    assert failed.error.message == "Не удалось сгенерировать изображение"


async def test_completed_publish_failure_requeues_without_failed_event() -> None:
    publisher = FakePublisher(fail_on="generation.completed")
    message = command_message()
    worker = GenerationWorker(FakeGenerator(), FakeStorage(), publisher)  # type: ignore[arg-type]

    await worker.handle(message)  # type: ignore[arg-type]

    assert message.acked is False
    assert message.nacked is True
    assert all(not isinstance(event, FailedEvent) for event, _ in publisher.events)


async def test_ack_failure_after_completed_event_requeues() -> None:
    publisher = FakePublisher()
    message = command_message()
    message.ack_error = OSError("channel closed")
    worker = GenerationWorker(FakeGenerator(), FakeStorage(), publisher)  # type: ignore[arg-type]

    await worker.handle(message)  # type: ignore[arg-type]

    assert message.acked is False
    assert message.nacked is True
    assert isinstance(publisher.events[-1][0], CompletedEvent)


async def test_malformed_message_is_rejected() -> None:
    message = FakeMessage(b"not-json")
    worker = GenerationWorker(FakeGenerator(), FakeStorage(), FakePublisher())  # type: ignore[arg-type]

    await worker.handle(message)  # type: ignore[arg-type]

    assert message.rejected is True
