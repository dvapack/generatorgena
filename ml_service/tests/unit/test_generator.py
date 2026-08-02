from io import BytesIO
from pathlib import Path

import torch
from PIL import Image

from generatorgena_ml.generation import GeneratorNetwork, TorchImageGenerator


def test_tensor_to_png_normalizes_tanh_output() -> None:
    tensor = torch.tensor(
        [
            [[-1.0, 1.0], [-1.0, 1.0]],
            [[-1.0, 1.0], [-1.0, 1.0]],
            [[-1.0, 1.0], [-1.0, 1.0]],
        ]
    )

    generated = TorchImageGenerator.tensor_to_png(tensor)
    image = Image.open(BytesIO(generated.data))

    assert generated.content_type == "image/png"
    assert generated.size_bytes == len(generated.data)
    assert (generated.width, generated.height) == (2, 2)
    assert image.getpixel((0, 0)) == (0, 0, 0)
    assert image.getpixel((1, 0)) == (255, 255, 255)


def test_network_is_compatible_with_packaged_checkpoint() -> None:
    checkpoint = Path(__file__).parents[2] / "Generator_70.pt"
    state = torch.load(checkpoint, map_location="cpu", weights_only=True)

    GeneratorNetwork().load_state_dict(state)
