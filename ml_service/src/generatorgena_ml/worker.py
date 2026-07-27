from __future__ import annotations

import asyncio
import json
import logging
from typing import Protocol

from aio_pika import IncomingMessage
from pydantic import ValidationError

from generatorgena_ml.domain.contracts import (
    AssetPayload,
    CompletedEvent,
    ErrorPayload,
    FailedEvent,
    GenerateContentCommand,
    GenerationEvent,
    GenerationType,
    ProcessingEvent,
)
from generatorgena_ml.domain.errors import MessagingError
from generatorgena_ml.generation.generator import GeneratedImage
from generatorgena_ml.storage.minio import MinioStorage, StorageError


logger = logging.getLogger(__name__)


class ImageGenerator(Protocol):
    def generate_from_text(self, prompt: str) -> GeneratedImage: ...


class EventPublisher(Protocol):
    async def publish_event(self, event: GenerationEvent, routing_key: str) -> None: ...


class GenerationWorker:
    def __init__(
        self,
        generator: ImageGenerator,
        storage: MinioStorage,
        event_publisher: EventPublisher,
    ):
        self._generator = generator
        self._storage = storage
        self._event_publisher = event_publisher

    async def handle(self, message: IncomingMessage) -> None:
        try:
            command = GenerateContentCommand.model_validate_json(message.body)
        except (ValidationError, ValueError, json.JSONDecodeError):
            logger.error("Получена некорректная команда на генерацию", exc_info=True)
            await message.reject(requeue=False)
            return

        context = {
            "command_id": str(command.command_id),
            "generation_id": str(command.generation_id),
        }
        logger.info("Получена команда на генерацию", extra=context)

        try:
            await self._event_publisher.publish_event(
                ProcessingEvent(
                    commandId=command.command_id,
                    generationId=command.generation_id,
                ),
                "generation.processing",
            )

            if command.type is not GenerationType.IMAGE:
                await self._publish_failure(
                    command,
                    code="UNSUPPORTED_TYPE",
                    message=f"Тип генерации {command.type.value} не поддерживается",
                    retryable=False,
                )
                await self._ack(message)
                return

            image = await asyncio.to_thread(
                self._generator.generate_from_text,
                command.prompt,
            )
            object_key = self.object_key(command.generation_id)
            await self._storage.put_image(object_key, image)
            await self._event_publisher.publish_event(
                CompletedEvent(
                    commandId=command.command_id,
                    generationId=command.generation_id,
                    asset=AssetPayload(
                        objectKey=object_key,
                        sizeBytes=image.size_bytes,
                        width=image.width,
                        height=image.height,
                    ),
                ),
                "generation.completed",
            )
            await self._ack(message)
            logger.info("Команда на генерацию успешно выполнена", extra=context)
        except MessagingError:
            logger.error(
                "Не удалось опубликовать событие в RabbitMQ",
                extra=context,
                exc_info=True,
            )
            await self._requeue(message)
        except StorageError as exception:
            logger.error(
                "Не удалось сохранить сгенерированное изображение",
                extra=context,
                exc_info=True,
            )
            await self._finish_with_failure(
                message,
                command,
                code="STORAGE_ERROR",
                error_message="Не удалось сохранить сгенерированное изображение",
                retryable=exception.retryable,
            )
        except Exception:
            logger.error(
                "Ошибка при выполнении команды на генерацию",
                extra=context,
                exc_info=True,
            )
            await self._finish_with_failure(
                message,
                command,
                code="GENERATION_ERROR",
                error_message="Не удалось сгенерировать изображение",
                retryable=False,
            )

    async def _finish_with_failure(
        self,
        incoming_message: IncomingMessage,
        command: GenerateContentCommand,
        *,
        code: str,
        error_message: str,
        retryable: bool,
    ) -> None:
        try:
            await self._publish_failure(
                command,
                code=code,
                message=error_message,
                retryable=retryable,
            )
            await self._ack(incoming_message)
        except MessagingError:
            logger.error(
                "Не удалось опубликовать событие об ошибке генерации",
                exc_info=True,
            )
            await self._requeue(incoming_message)

    async def _publish_failure(
        self,
        command: GenerateContentCommand,
        *,
        code: str,
        message: str,
        retryable: bool,
    ) -> None:
        await self._event_publisher.publish_event(
            FailedEvent(
                commandId=command.command_id,
                generationId=command.generation_id,
                error=ErrorPayload(
                    code=code,
                    message=message,
                    retryable=retryable,
                ),
            ),
            "generation.failed",
        )

    @staticmethod
    def object_key(generation_id: object) -> str:
        return f"images/requests/{generation_id}/result.png"

    @staticmethod
    async def _requeue(message: IncomingMessage) -> None:
        if message.processed:
            return
        try:
            await message.nack(requeue=True)
        except Exception:
            logger.warning(
                "Не удалось вернуть команду на генерацию в очередь",
                exc_info=True,
            )

    @staticmethod
    async def _ack(message: IncomingMessage) -> None:
        try:
            await message.ack()
        except Exception as exception:
            raise MessagingError(
                "Не удалось подтвердить обработку команды RabbitMQ"
            ) from exception
