"""Image generation port."""

from typing import Protocol

from generatorgena_ml.model import GeneratedImage


class ImageGenerator(Protocol):
    async def generate(self, prompt: str) -> GeneratedImage: ...
