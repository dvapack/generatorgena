"""MinIO implementation of the generated asset repository."""

from __future__ import annotations

import asyncio
import logging
from io import BytesIO

from minio import Minio
from minio.error import InvalidResponseError, S3Error, ServerError
from urllib3.exceptions import HTTPError

from generatorgena_ml.configuration.settings import MinioProperties
from generatorgena_ml.exception import AssetStorageError
from generatorgena_ml.model import GeneratedImage

logger = logging.getLogger(__name__)


class MinioGeneratedAssetRepository:
    _PERMANENT_S3_ERROR_CODES = frozenset(
        {
            "AccessDenied",
            "InvalidAccessKeyId",
            "InvalidBucketName",
            "InvalidRequest",
            "NoSuchBucket",
            "SignatureDoesNotMatch",
        }
    )

    def __init__(
        self,
        client: Minio,
        bucket: str,
        max_attempts: int = 3,
    ):
        self._client = client
        self._bucket = bucket
        self._max_attempts = max_attempts

    @classmethod
    def from_properties(
        cls,
        properties: MinioProperties,
    ) -> MinioGeneratedAssetRepository:
        client = Minio(
            endpoint=properties.endpoint,
            access_key=properties.access_key,
            secret_key=properties.secret_key,
            secure=properties.secure,
            region=properties.region,
        )
        return cls(
            client=client,
            bucket=properties.bucket,
            max_attempts=properties.max_attempts,
        )

    async def ensure_ready(self) -> None:
        try:
            exists = await asyncio.to_thread(
                self._client.bucket_exists,
                self._bucket,
            )
        except Exception as exception:
            raise AssetStorageError(
                "Хранилище MinIO недоступно",
                retryable=True,
            ) from exception
        if not exists:
            raise AssetStorageError(
                f"Бакет MinIO {self._bucket!r} не существует",
                retryable=False,
            )

    async def is_ready(self) -> bool:
        try:
            return await asyncio.to_thread(
                self._client.bucket_exists,
                self._bucket,
            )
        except Exception:
            return False

    async def save(self, object_key: str, image: GeneratedImage) -> None:
        for attempt in range(1, self._max_attempts + 1):
            try:
                await asyncio.to_thread(
                    self._client.put_object,
                    self._bucket,
                    object_key,
                    BytesIO(image.data),
                    image.size_bytes,
                    content_type=image.content_type,
                )
                return
            except Exception as exception:
                retryable = self._is_retryable(exception)
                if not retryable or attempt == self._max_attempts:
                    raise AssetStorageError(
                        "Не удалось загрузить сгенерированное изображение в MinIO",
                        retryable=retryable,
                    ) from exception
                delay_seconds = 2 ** (attempt - 1)
                logger.warning(
                    "Ошибка MinIO. Повторная попытка сохранения изображения",
                    extra={
                        "attempt": attempt,
                        "delay_seconds": delay_seconds,
                    },
                    exc_info=exception,
                )
                await asyncio.sleep(delay_seconds)

    @classmethod
    def _is_retryable(cls, exception: Exception) -> bool:
        if isinstance(exception, S3Error):
            return exception.code not in cls._PERMANENT_S3_ERROR_CODES
        return isinstance(
            exception,
            InvalidResponseError | ServerError | HTTPError | OSError,
        )
