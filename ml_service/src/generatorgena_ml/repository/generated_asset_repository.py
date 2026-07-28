"""Generated asset repository port."""

from typing import Protocol

from generatorgena_ml.model import GeneratedImage


class GeneratedAssetRepository(Protocol):
    async def save(self, object_key: str, image: GeneratedImage) -> None: ...

    async def is_ready(self) -> bool: ...
