"""Generation event publication port."""

from typing import Protocol

from generatorgena_ml.model import GenerationEvent


class GenerationEventPublisher(Protocol):
    async def publish(self, event: GenerationEvent) -> None: ...
