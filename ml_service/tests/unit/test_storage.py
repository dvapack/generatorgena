from unittest.mock import MagicMock

import pytest

from generatorgena_ml.exception import AssetStorageError
from generatorgena_ml.model import GeneratedImage
from generatorgena_ml.repository import MinioGeneratedAssetRepository


@pytest.fixture
def image() -> GeneratedImage:
    return GeneratedImage(
        data=b"png-data",
        content_type="image/png",
        size_bytes=8,
        width=64,
        height=64,
    )


async def test_put_image_uses_minio_sdk(image: GeneratedImage) -> None:
    client = MagicMock()
    storage = MinioGeneratedAssetRepository(client=client, bucket="generated-assets")

    await storage.save("images/requests/id/result.png", image)

    args, kwargs = client.put_object.call_args
    assert args[0] == "generated-assets"
    assert args[1] == "images/requests/id/result.png"
    assert args[3] == 8
    assert kwargs == {"content_type": "image/png"}
    assert args[2].read() == b"png-data"


async def test_put_image_retries_transient_errors(
    image: GeneratedImage,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    client = MagicMock()
    client.put_object.side_effect = [OSError("down"), OSError("down"), object()]
    storage = MinioGeneratedAssetRepository(
        client=client,
        bucket="generated-assets",
        max_attempts=3,
    )

    async def no_sleep(_: float) -> None:
        return None

    monkeypatch.setattr(
        "generatorgena_ml.repository.minio_generated_asset_repository.asyncio.sleep",
        no_sleep,
    )
    await storage.save("images/requests/id/result.png", image)

    assert client.put_object.call_count == 3


async def test_put_image_raises_after_retry_limit(
    image: GeneratedImage,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    client = MagicMock()
    client.put_object.side_effect = OSError("down")
    storage = MinioGeneratedAssetRepository(
        client=client,
        bucket="generated-assets",
        max_attempts=3,
    )

    async def no_sleep(_: float) -> None:
        return None

    monkeypatch.setattr(
        "generatorgena_ml.repository.minio_generated_asset_repository.asyncio.sleep",
        no_sleep,
    )

    with pytest.raises(AssetStorageError) as error:
        await storage.save("images/requests/id/result.png", image)

    assert error.value.retryable is True
    assert str(error.value) == (
        "Не удалось загрузить сгенерированное изображение в MinIO"
    )
    assert client.put_object.call_count == 3


async def test_readiness_requires_existing_bucket() -> None:
    client = MagicMock()
    client.bucket_exists.return_value = True
    storage = MinioGeneratedAssetRepository(client=client, bucket="generated-assets")

    assert await storage.is_ready() is True
    client.bucket_exists.assert_called_once_with("generated-assets")
