"""Composition root and FastAPI application lifecycle."""

from __future__ import annotations

import asyncio
import logging
from collections.abc import AsyncIterator, Callable
from contextlib import asynccontextmanager

from fastapi import FastAPI

from generatorgena_ml.configuration.settings import (
    ApplicationSettings,
    get_settings,
)
from generatorgena_ml.controller import (
    HealthController,
    RabbitGenerationCommandController,
)
from generatorgena_ml.generation import TorchImageGenerator
from generatorgena_ml.mapper import GenerationMessageMapper
from generatorgena_ml.messaging.rabbit_generation_event_publisher import (
    RabbitGenerationEventPublisher,
)
from generatorgena_ml.messaging.rabbitmq_manager import RabbitMqManager
from generatorgena_ml.repository import MinioGeneratedAssetRepository
from generatorgena_ml.service import GenerationService

logger = logging.getLogger(__name__)
SettingsProvider = Callable[[], ApplicationSettings]


class ApplicationContext:
    """Owns singleton dependencies"""

    def __init__(self, settings_provider: SettingsProvider):
        self._settings_provider = settings_provider
        self._model_loaded = False
        self._repository: MinioGeneratedAssetRepository | None = None
        self._rabbitmq: RabbitMqManager | None = None

    async def start(self) -> None:
        settings = self._settings_provider()
        logger.info("Загрузка модели генерации")
        model = await asyncio.to_thread(
            TorchImageGenerator,
            settings.model.checkpoint_path,
            settings.model.clip_model_name,
        )
        self._model_loaded = True

        repository = MinioGeneratedAssetRepository.from_properties(settings.minio)
        await repository.ensure_ready()
        self._repository = repository

        mapper = GenerationMessageMapper()
        rabbitmq = RabbitMqManager(settings.rabbitmq)
        publisher = RabbitGenerationEventPublisher(rabbitmq, mapper)
        service = GenerationService(model, repository, publisher)
        controller = RabbitGenerationCommandController(service, mapper)
        self._rabbitmq = rabbitmq
        try:
            await rabbitmq.start(controller.handle)
        except Exception:
            await rabbitmq.close()
            self._rabbitmq = None
            raise
        logger.info("ML-сервис готов к работе")

    async def stop(self) -> None:
        self._model_loaded = False
        if self._rabbitmq is not None:
            await self._rabbitmq.close()
        self._rabbitmq = None
        self._repository = None
        logger.info("ML-сервис остановлен")

    async def is_ready(self) -> bool:
        if (
            not self._model_loaded
            or self._rabbitmq is None
            or not self._rabbitmq.is_ready
            or self._repository is None
        ):
            return False
        return await self._repository.is_ready()


def create_application(
    settings_provider: SettingsProvider = get_settings,
) -> FastAPI:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    context = ApplicationContext(settings_provider)

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncIterator[None]:
        try:
            await context.start()
            yield
        finally:
            await context.stop()

    application = FastAPI(
        title="ML-сервис GeneratorGena",
        lifespan=lifespan,
    )
    application.include_router(HealthController(context).router)
    return application
