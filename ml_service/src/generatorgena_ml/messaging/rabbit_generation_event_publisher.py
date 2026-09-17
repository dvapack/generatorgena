"""RabbitMQ adapter for generation events."""

import json
from typing import ClassVar

from generatorgena_ml.mapper import GenerationMessageMapper
from generatorgena_ml.messaging.rabbitmq_manager import RabbitMqManager
from generatorgena_ml.model import (
    CompletedEvent,
    FailedEvent,
    GenerationEvent,
    ProcessingEvent,
)


class RabbitGenerationEventPublisher:
    _ROUTING_KEYS: ClassVar[dict[type[object], str]] = {
        ProcessingEvent: "generation.processing",
        CompletedEvent: "generation.completed",
        FailedEvent: "generation.failed",
    }

    def __init__(
        self,
        rabbitmq: RabbitMqManager,
        mapper: GenerationMessageMapper,
    ):
        self._rabbitmq = rabbitmq
        self._mapper = mapper

    async def publish(self, event: GenerationEvent) -> None:
        dto = self._mapper.to_event_dto(event)
        payload = dto.model_dump(mode="json", by_alias=True, exclude_none=False)
        await self._rabbitmq.publish(
            body=json.dumps(payload, separators=(",", ":")).encode(),
            routing_key=self._ROUTING_KEYS[type(event)],
            event_id=str(event.event_id),
            command_id=str(event.command_id),
        )
