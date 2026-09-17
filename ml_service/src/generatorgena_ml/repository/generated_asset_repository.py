"""Generated asset repository port."""

from typing import Protocol

from generatorgena_ml.model import GeneratedAsset, GeneratedImage


class GeneratedAssetRepository(Protocol):
    async def find(self, object_key: str) -> GeneratedAsset | None: ...

    async def save(self, object_key: str, image: GeneratedImage) -> None: ...

    async def is_ready(self) -> bool: ...
