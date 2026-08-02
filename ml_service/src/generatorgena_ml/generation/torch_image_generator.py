"""PyTorch and CLIP implementation of the image generation port."""

from __future__ import annotations

import asyncio
from io import BytesIO
from pathlib import Path
from typing import cast

import torch
from PIL import Image
from torch import nn
from torchvision.transforms.functional import to_pil_image
from transformers import CLIPTextModel, CLIPTokenizer

from generatorgena_ml.exception import ImageGenerationError
from generatorgena_ml.model import GeneratedImage


class GeneratorNetwork(nn.Module):
    def __init__(
        self,
        noise_dimension: int = 100,
        text_dimension: int = 512,
        output_channels: int = 3,
    ):
        super().__init__()
        self.projection = nn.Linear(text_dimension, 256)
        # Attribute names are part of the serialized checkpoint contract.
        self.fc = nn.Linear(noise_dimension + 256, 512 * 4 * 4)
        self.net = nn.Sequential(
            nn.BatchNorm2d(512),
            nn.ConvTranspose2d(512, 256, 4, 2, 1),
            nn.BatchNorm2d(256),
            nn.ReLU(True),
            nn.ConvTranspose2d(256, 128, 4, 2, 1),
            nn.BatchNorm2d(128),
            nn.ReLU(True),
            nn.ConvTranspose2d(128, 64, 4, 2, 1),
            nn.BatchNorm2d(64),
            nn.ReLU(True),
            nn.ConvTranspose2d(64, output_channels, 4, 2, 1),
            nn.Tanh(),
        )

    def forward(
        self,
        noise: torch.Tensor,
        text_embedding: torch.Tensor,
    ) -> torch.Tensor:
        projected_text = self.projection(text_embedding)
        combined_input = torch.cat([noise, projected_text], dim=1)
        feature_map = self.fc(combined_input).view(-1, 512, 4, 4)
        return cast(torch.Tensor, self.net(feature_map))


class TorchImageGenerator:
    NOISE_DIMENSION = 100

    def __init__(
        self,
        checkpoint_path: str | Path,
        clip_model_name: str,
    ):
        self._device = torch.device("cpu")
        self._tokenizer = CLIPTokenizer.from_pretrained(clip_model_name)
        self._text_encoder = CLIPTextModel.from_pretrained(clip_model_name).to(
            self._device
        )
        self._text_encoder.eval()
        self._network = GeneratorNetwork().to(self._device)
        self._network.load_state_dict(
            torch.load(
                Path(checkpoint_path),
                map_location=self._device,
                weights_only=True,
            )
        )
        self._network.eval()

    async def generate(self, prompt: str) -> GeneratedImage:
        try:
            return await asyncio.to_thread(self._generate, prompt)
        except Exception as exception:
            raise ImageGenerationError(
                "Не удалось сгенерировать изображение"
            ) from exception

    def _generate(self, prompt: str) -> GeneratedImage:
        with torch.inference_mode():
            text_embedding = self._encode_text([prompt])
            noise = torch.randn(
                1,
                self.NOISE_DIMENSION,
                device=self._device,
            )
            generated_tensor = self._network(noise, text_embedding)[0]
        return self.tensor_to_png(generated_tensor)

    def _encode_text(self, captions: list[str]) -> torch.Tensor:
        tokenized = self._tokenizer(
            captions,
            padding=True,
            return_tensors="pt",
            truncation=True,
        )
        inputs = {name: value.to(self._device) for name, value in tokenized.items()}
        with torch.inference_mode():
            embedding = self._text_encoder(**inputs).last_hidden_state[:, 0, :]
            return cast(torch.Tensor, embedding)

    @staticmethod
    def tensor_to_png(image_tensor: torch.Tensor) -> GeneratedImage:
        normalized = ((image_tensor.detach().cpu() + 1.0) / 2.0).clamp(0, 1)
        image: Image.Image = to_pil_image(normalized)
        buffer = BytesIO()
        image.save(buffer, format="PNG")
        data = buffer.getvalue()
        return GeneratedImage(
            data=data,
            content_type="image/png",
            size_bytes=len(data),
            width=image.width,
            height=image.height,
        )
