from __future__ import annotations

import asyncio
from io import BytesIO
import logging

from minio import Minio
from minio.error import InvalidResponseError, S3Error, ServerError
from urllib3.exceptions import HTTPError

from generatorgena_ml.config import Settings
from generatorgena_ml.generation.generator import GeneratedImage


logger = logging.getLogger(__name__)


class StorageError(RuntimeError):
    def __init__(self, message: str, *, retryable: bool):
        super().__init__(message)
        self.retryable = retryable


class MinioStorage:
    _PERMANENT_S3_ERROR_CODES = {
        "AccessDenied",
        "InvalidAccessKeyId",
        "InvalidBucketName",
        "InvalidRequest",
        "NoSuchBucket",
        "SignatureDoesNotMatch",
    }

    def __init__(
        self,
        client: Minio,
        bucket: str,
        max_attempts: int = 3,
    ):
        self._client = client
        self.bucket = bucket
        self.max_attempts = max_attempts

    @classmethod
    def from_settings(cls, settings: Settings) -> "MinioStorage":
        client = Minio(
            endpoint=settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
            region=settings.minio_region,
        )
        return cls(
            client=client,
            bucket=settings.minio_bucket,
            max_attempts=settings.minio_max_attempts,
        )

    async def ensure_ready(self) -> None:
        try:
            exists = await asyncio.to_thread(self._client.bucket_exists, self.bucket)
        except Exception as exception:
            raise StorageError("Хранилище MinIO недоступно", retryable=True) from exception
        if not exists:
            raise StorageError(
                f"Бакет MinIO {self.bucket!r} не существует",
                retryable=False,
            )

    async def is_ready(self) -> bool:
        try:
            return await asyncio.to_thread(self._client.bucket_exists, self.bucket)
        except Exception:
            return False

    async def put_image(self, object_key: str, image: GeneratedImage) -> None:
        for attempt in range(1, self.max_attempts + 1):
            try:
                await asyncio.to_thread(
                    self._client.put_object,
                    self.bucket,
                    object_key,
                    BytesIO(image.data),
                    image.size_bytes,
                    content_type=image.content_type,
                )
                return
            except Exception as exception:
                retryable = self._is_retryable(exception)
                if not retryable or attempt == self.max_attempts:
                    raise StorageError(
                        "Не удалось загрузить сгенерированное изображение в MinIO",
                        retryable=retryable,
                    ) from exception
                delay = 2 ** (attempt - 1)
                logger.warning(
                    "Ошибка при загрузке изображения в MinIO. Повторная попытка",
                    extra={"attempt": attempt, "delay_seconds": delay},
                    exc_info=exception,
                )
                await asyncio.sleep(delay)

    @classmethod
    def _is_retryable(cls, exception: Exception) -> bool:
        if isinstance(exception, S3Error):
            return exception.code not in cls._PERMANENT_S3_ERROR_CODES
        if isinstance(exception, (InvalidResponseError, ServerError, HTTPError, OSError)):
            return True
        return False
