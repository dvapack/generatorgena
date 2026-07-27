from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, status

from generatorgena_ml.config import get_settings
from generatorgena_ml.generation.generator import Gena
from generatorgena_ml.messaging.rabbitmq import RabbitMessaging
from generatorgena_ml.storage.minio import MinioStorage
from generatorgena_ml.worker import GenerationWorker


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    logger.info("Загрузка модели генерации")
    model = await asyncio.to_thread(
        Gena,
        settings.model_checkpoint_path,
        settings.clip_model_name,
    )
    storage = MinioStorage.from_settings(settings)
    await storage.ensure_ready()

    messaging = RabbitMessaging(settings)
    worker = GenerationWorker(
        generator=model,
        storage=storage,
        event_publisher=messaging,
    )
    await messaging.start(worker.handle)

    app.state.model_loaded = True
    app.state.storage = storage
    app.state.messaging = messaging
    logger.info("ML-сервис готов к работе")
    try:
        yield
    finally:
        app.state.model_loaded = False
        await messaging.close()
        logger.info("ML-сервис остановлен")


app = FastAPI(title="ML-сервис GeneratorGena", lifespan=lifespan)


@app.get("/health/live")
async def liveness() -> dict[str, str]:
    return {"status": "UP"}


@app.get("/health/ready")
async def readiness() -> dict[str, str]:
    model_loaded = getattr(app.state, "model_loaded", False)
    messaging: RabbitMessaging | None = getattr(app.state, "messaging", None)
    storage: MinioStorage | None = getattr(app.state, "storage", None)

    storage_ready = storage is not None and await storage.is_ready()
    messaging_ready = messaging is not None and messaging.is_ready
    if not model_loaded or not messaging_ready or not storage_ready:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="ML-сервис не готов к работе",
        )
    return {"status": "UP"}
