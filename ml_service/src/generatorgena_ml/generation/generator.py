"""PyTorch image generator and PNG conversion."""

from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO
from pathlib import Path

import torch
from PIL import Image
from torch import nn
from torchvision.transforms.functional import to_pil_image
from transformers import CLIPTextModel, CLIPTokenizer


@dataclass(frozen=True, slots=True)
class GeneratedImage:
    data: bytes
    content_type: str
    size_bytes: int
    width: int
    height: int


class Generator(nn.Module):
    def __init__(self, noise_dim: int = 100, text_dim: int = 512, out_channels: int = 3):
        super().__init__()
        self.projection = nn.Linear(text_dim, 256)
        self.fc = nn.Linear(noise_dim + 256, 512 * 4 * 4)

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
            nn.ConvTranspose2d(64, out_channels, 4, 2, 1),
            nn.Tanh(),
        )

    def forward(self, noise: torch.Tensor, text_emb: torch.Tensor) -> torch.Tensor:
        projected_text = self.projection(text_emb)
        x = torch.cat([noise, projected_text], dim=1)
        x = self.fc(x).view(-1, 512, 4, 4)
        return self.net(x)


class Gena:
    def __init__(
        self,
        checkpoint_path: str | Path | None = None,
        clip_model_name: str = "openai/clip-vit-base-patch32",
    ):
        self.device = torch.device("cpu")
        default_checkpoint = Path(__file__).parents[3] / "Generator_70.pt"
        self.checkpoint_path = Path(checkpoint_path or default_checkpoint)
        self.tokenizer = CLIPTokenizer.from_pretrained(clip_model_name)
        self.text_encoder = CLIPTextModel.from_pretrained(clip_model_name).to(self.device)
        self.text_encoder.eval()
        self.generator = Generator().to(self.device)
        self.generator.load_state_dict(
            torch.load(
                self.checkpoint_path,
                map_location=self.device,
                weights_only=True,
            )
        )
        self.generator.eval()

    def encode_text(self, captions: list[str]) -> torch.Tensor:
        inputs = self.tokenizer(
            captions,
            padding=True,
            return_tensors="pt",
            truncation=True,
        )
        inputs = {key: value.to(self.device) for key, value in inputs.items()}
        with torch.inference_mode():
            return self.text_encoder(**inputs).last_hidden_state[:, 0, :]

    @staticmethod
    def tensor_to_png(img_tensor: torch.Tensor) -> GeneratedImage:
        normalized = ((img_tensor.detach().cpu() + 1.0) / 2.0).clamp(0, 1)
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

    def generate_from_text(self, prompt: str) -> GeneratedImage:
        with torch.inference_mode():
            text_embedding = self.encode_text([prompt])
            noise = torch.randn(1, 100, device=self.device)
            generated = self.generator(noise, text_embedding)[0]
        return self.tensor_to_png(generated)
