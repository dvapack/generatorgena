"""Mappings for RabbitMQ generation messages."""

from generatorgena_ml.dto import (
    AssetPayload,
    CompletedEventDto,
    ErrorPayload,
    FailedEventDto,
    GenerateContentCommandDto,
    GenerationEventDto,
    ProcessingEventDto,
)
from generatorgena_ml.model import (
    CompletedEvent,
    FailedEvent,
    GenerationCommand,
    GenerationEvent,
    ProcessingEvent,
)


class GenerationMessageMapper:
    @staticmethod
    def to_command(dto: GenerateContentCommandDto) -> GenerationCommand:
        return GenerationCommand(
            command_id=dto.command_id,
            generation_id=dto.generation_id,
            prompt=dto.prompt,
            generation_type=dto.generation_type,
        )

    @staticmethod
    def to_event_dto(event: GenerationEvent) -> GenerationEventDto:
        if isinstance(event, ProcessingEvent):
            return ProcessingEventDto(
                event_id=event.event_id,
                command_id=event.command_id,
                generation_id=event.generation_id,
                status="PROCESSING",
                occurred_at=event.occurred_at,
            )
        if isinstance(event, CompletedEvent):
            return CompletedEventDto(
                event_id=event.event_id,
                command_id=event.command_id,
                generation_id=event.generation_id,
                status="COMPLETED",
                occurred_at=event.occurred_at,
                asset=AssetPayload(
                    object_key=event.asset.object_key,
                    asset_type="IMAGE",
                    content_type="image/png",
                    size_bytes=event.asset.size_bytes,
                    width=event.asset.width,
                    height=event.asset.height,
                    duration=event.asset.duration,
                ),
            )
        if isinstance(event, FailedEvent):
            return FailedEventDto(
                event_id=event.event_id,
                command_id=event.command_id,
                generation_id=event.generation_id,
                status="FAILED",
                occurred_at=event.occurred_at,
                error=ErrorPayload(
                    code=event.code,
                    message=event.message,
                    retryable=event.retryable,
                ),
            )
        raise TypeError(f"Неподдерживаемый тип события: {type(event).__name__}")
