from io import BytesIO

import torch
from PIL import Image

from generatorgena_ml.generation.generator import Gena


def test_tensor_to_png_normalizes_tanh_output() -> None:
    tensor = torch.tensor(
        [
            [[-1.0, 1.0], [-1.0, 1.0]],
            [[-1.0, 1.0], [-1.0, 1.0]],
            [[-1.0, 1.0], [-1.0, 1.0]],
        ]
    )

    generated = Gena.tensor_to_png(tensor)
    image = Image.open(BytesIO(generated.data))

    assert generated.content_type == "image/png"
    assert generated.size_bytes == len(generated.data)
    assert (generated.width, generated.height) == (2, 2)
    assert image.getpixel((0, 0)) == (0, 0, 0)
    assert image.getpixel((1, 0)) == (255, 255, 255)
