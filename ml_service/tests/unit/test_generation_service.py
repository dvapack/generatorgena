from dataclasses import dataclass, field
from uuid import uuid4

from generatorgena_ml.exception import (
    AssetStorageError,
    EventPublicationError,
    ImageGenerationError,
)
from generatorgena_ml.model import (
    CompletedEvent,
    FailedEvent,
    GeneratedImage,
    GenerationCommand,
    GenerationEvent,
    ProcessingEvent,
)
from generatorgena_ml.service import GenerationService


class FakeGenerator:
    def __init__(self, error: Exception | None = None):
        self.error = error

    async def generate(self, prompt: str) -> GeneratedImage:
        if self.error:
            raise self.error
        return GeneratedImage(b"png", "image/png", 3, 64, 64)


class FakeRepository:
    def __init__(self, error: Exception | None = None):
        self.error = error
        self.uploads: list[tuple[str, GeneratedImage]] = []

    async def save(self, object_key: str, image: GeneratedImage) -> None:
        if self.error:
            raise self.error
        self.uploads.append((object_key, image))

    async def is_ready(self) -> bool:
        return True


@dataclass
class FakePublisher:
    fail_on: type[GenerationEvent] | None = None
    events: list[GenerationEvent] = field(default_factory=list)

    async def publish(self, event: GenerationEvent) -> None:
        if self.fail_on is not None and isinstance(event, self.fail_on):
            raise EventPublicationError("publish failed")
        self.events.append(event)


def command() -> GenerationCommand:
    return GenerationCommand(
        command_id=uuid4(),
        generation_id=uuid4(),
        prompt="flowers",
    )


async def test_success_uploads_then_publishes_completed() -> None:
    repository = FakeRepository()
    publisher = FakePublisher()
    request = command()
    service = GenerationService(FakeGenerator(), repository, publisher)

    await service.generate(request)

    assert repository.uploads[0][0] == (
        f"images/requests/{request.generation_id}/result.png"
    )
    assert [type(event) for event in publisher.events] == [
        ProcessingEvent,
        CompletedEvent,
    ]


async def test_storage_error_is_mapped_to_failed_event() -> None:
    publisher = FakePublisher()
    repository = FakeRepository(AssetStorageError("down", retryable=True))
    service = GenerationService(FakeGenerator(), repository, publisher)

    await service.generate(command())

    failed = publisher.events[-1]
    assert isinstance(failed, FailedEvent)
    assert failed.code == "STORAGE_ERROR"
    assert failed.retryable is True


async def test_generation_error_is_mapped_to_failed_event() -> None:
    publisher = FakePublisher()
    generator = FakeGenerator(ImageGenerationError("model failed"))
    service = GenerationService(generator, FakeRepository(), publisher)

    await service.generate(command())

    failed = publisher.events[-1]
    assert isinstance(failed, FailedEvent)
    assert failed.code == "GENERATION_ERROR"
    assert failed.message == "Не удалось сгенерировать изображение"


async def test_event_publication_error_crosses_service_boundary() -> None:
    publisher = FakePublisher(fail_on=CompletedEvent)
    service = GenerationService(FakeGenerator(), FakeRepository(), publisher)

    try:
        await service.generate(command())
    except EventPublicationError:
        pass
    else:
        raise AssertionError("EventPublicationError was expected")
