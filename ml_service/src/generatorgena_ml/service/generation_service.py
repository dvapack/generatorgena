"""Generation use-case orchestration."""

from __future__ import annotations

import logging
from uuid import UUID

from generatorgena_ml.exception import AssetStorageError, ImageGenerationError
from generatorgena_ml.generation.image_generator import ImageGenerator
from generatorgena_ml.messaging.generation_event_publisher import (
    GenerationEventPublisher,
)
from generatorgena_ml.model import (
    CompletedEvent,
    FailedEvent,
    FailureCode,
    GeneratedAsset,
    GenerationCommand,
    ProcessingEvent,
)
from generatorgena_ml.repository.generated_asset_repository import (
    GeneratedAssetRepository,
)

logger = logging.getLogger(__name__)


class GenerationService:
    def __init__(
        self,
        image_generator: ImageGenerator,
        asset_repository: GeneratedAssetRepository,
        event_publisher: GenerationEventPublisher,
    ):
        self._image_generator = image_generator
        self._asset_repository = asset_repository
        self._event_publisher = event_publisher

    async def generate(self, command: GenerationCommand) -> None:
        context = {
            "command_id": str(command.command_id),
            "generation_id": str(command.generation_id),
        }
        logger.info("Получена команда на генерацию", extra=context)
        await self._event_publisher.publish(
            ProcessingEvent(
                command_id=command.command_id,
                generation_id=command.generation_id,
            )
        )

        try:
            image = await self._image_generator.generate(command.prompt)
        except ImageGenerationError:
            logger.error("Ошибка ML-модели при генерации изображения", extra=context)
            await self._publish_failure(
                command,
                code=FailureCode.GENERATION_ERROR,
                message="Не удалось сгенерировать изображение",
                retryable=False,
            )
            return
        except Exception:
            logger.exception("Неожиданная ошибка генератора изображений", extra=context)
            await self._publish_failure(
                command,
                code=FailureCode.GENERATION_ERROR,
                message="Не удалось сгенерировать изображение",
                retryable=False,
            )
            return

        object_key = self.object_key(command.generation_id)
        try:
            await self._asset_repository.save(object_key, image)
        except AssetStorageError as exception:
            logger.error(
                "Не удалось сохранить сгенерированное изображение",
                extra=context,
                exc_info=True,
            )
            await self._publish_failure(
                command,
                code=FailureCode.STORAGE_ERROR,
                message="Не удалось сохранить сгенерированное изображение",
                retryable=exception.retryable,
            )
            return

        await self._event_publisher.publish(
            CompletedEvent(
                command_id=command.command_id,
                generation_id=command.generation_id,
                asset=GeneratedAsset(
                    object_key=object_key,
                    size_bytes=image.size_bytes,
                    content_type=image.content_type,
                ),
            )
        )
        logger.info("Команда на генерацию успешно выполнена", extra=context)

    async def _publish_failure(
        self,
        command: GenerationCommand,
        *,
        code: FailureCode,
        message: str,
        retryable: bool,
    ) -> None:
        await self._event_publisher.publish(
            FailedEvent(
                command_id=command.command_id,
                generation_id=command.generation_id,
                code=code,
                message=message,
                retryable=retryable,
            )
        )

    @staticmethod
    def object_key(generation_id: UUID) -> str:
        return f"images/requests/{generation_id}/result.png"
