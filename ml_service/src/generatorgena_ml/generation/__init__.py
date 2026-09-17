"""Image generation port and lazily loaded PyTorch implementation."""

from typing import Any

from generatorgena_ml.generation.image_generator import ImageGenerator

__all__ = ["GeneratorNetwork", "ImageGenerator", "TorchImageGenerator"]


def __getattr__(name: str) -> Any:
    if name in {"GeneratorNetwork", "TorchImageGenerator"}:
        from generatorgena_ml.generation.torch_image_generator import (
            GeneratorNetwork,
            TorchImageGenerator,
        )

        implementations = {
            "GeneratorNetwork": GeneratorNetwork,
            "TorchImageGenerator": TorchImageGenerator,
        }
        return implementations[name]
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
