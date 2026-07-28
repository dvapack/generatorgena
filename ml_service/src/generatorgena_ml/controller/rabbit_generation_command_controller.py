"""RabbitMQ command listener and delivery acknowledgement policy."""

from __future__ import annotations

import logging

from aio_pika.abc import AbstractIncomingMessage
from pydantic import ValidationError

from generatorgena_ml.dto import GenerateContentCommandDto
from generatorgena_ml.exception import (
    EventPublicationError,
    MessageAcknowledgementError,
)
from generatorgena_ml.mapper import GenerationMessageMapper
from generatorgena_ml.service import GenerationService

logger = logging.getLogger(__name__)


class RabbitGenerationCommandController:
    def __init__(
        self,
        generation_service: GenerationService,
        mapper: GenerationMessageMapper,
    ):
        self._generation_service = generation_service
        self._mapper = mapper

    async def handle(self, message: AbstractIncomingMessage) -> None:
        try:
            dto = GenerateContentCommandDto.model_validate_json(message.body)
        except (ValidationError, ValueError):
            logger.error("Получена некорректная команда на генерацию", exc_info=True)
            await self._reject(message)
            return

        command = self._mapper.to_command(dto)
        context = {
            "command_id": str(command.command_id),
            "generation_id": str(command.generation_id),
        }
        try:
            await self._generation_service.generate(command)
            await self._ack(message)
        except (EventPublicationError, MessageAcknowledgementError):
            logger.error(
                "Команда не подтверждена из-за ошибки RabbitMQ",
                extra=context,
                exc_info=True,
            )
            await self._requeue(message)
        except Exception:
            logger.exception(
                "Неожиданная ошибка обработки команды",
                extra=context,
            )
            await self._requeue(message)

    @staticmethod
    async def _ack(message: AbstractIncomingMessage) -> None:
        try:
            await message.ack()
        except Exception as exception:
            raise MessageAcknowledgementError(
                "Не удалось подтвердить обработку команды RabbitMQ"
            ) from exception

    @staticmethod
    async def _reject(message: AbstractIncomingMessage) -> None:
        if message.processed:
            return
        try:
            await message.reject(requeue=False)
        except Exception:
            logger.warning(
                "Не удалось отклонить некорректную команду",
                exc_info=True,
            )

    @staticmethod
    async def _requeue(message: AbstractIncomingMessage) -> None:
        if message.processed:
            return
        try:
            await message.nack(requeue=True)
        except Exception:
            logger.warning(
                "Не удалось вернуть команду на генерацию в очередь",
                exc_info=True,
            )
