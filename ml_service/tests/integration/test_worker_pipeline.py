import asyncio
import json
import os
from io import BytesIO
from uuid import uuid4

import aio_pika
import pytest
from PIL import Image

from generatorgena_ml.config import Settings
from generatorgena_ml.generation.generator import GeneratedImage
from generatorgena_ml.messaging.rabbitmq import RabbitMessaging
from generatorgena_ml.storage.minio import MinioStorage
from generatorgena_ml.worker import GenerationWorker


pytestmark = pytest.mark.integration


class FakeGenerator:
    def generate_from_text(self, prompt: str) -> GeneratedImage:
        buffer = BytesIO()
        Image.new("RGB", (64, 64), color="green").save(buffer, format="PNG")
        data = buffer.getvalue()
        return GeneratedImage(data, "image/png", len(data), 64, 64)


async def wait_for_message(queue: aio_pika.abc.AbstractQueue) -> aio_pika.IncomingMessage:
    deadline = asyncio.get_running_loop().time() + 20
    while asyncio.get_running_loop().time() < deadline:
        message = await queue.get(fail=False, timeout=5)
        if message is not None:
            return message
        await asyncio.sleep(0.1)
    raise TimeoutError("Timed out waiting for a RabbitMQ event")


@pytest.mark.skipif(
    os.getenv("RUN_INTEGRATION_TESTS") != "1",
    reason="integration environment is not enabled",
)
async def test_rabbit_command_creates_minio_object() -> None:
    queue_suffix = uuid4().hex
    settings = Settings(
        generation_requests_queue=f"generation.requests.test.{queue_suffix}",
        generation_results_queue=f"generation.results.test.{queue_suffix}",
        generation_generate_routing_key=f"generation.generate.test.{queue_suffix}",
    )
    storage = MinioStorage.from_settings(settings)
    await storage.ensure_ready()
    messaging = RabbitMessaging(settings)
    worker = GenerationWorker(FakeGenerator(), storage, messaging)
    await messaging.start(worker.handle)

    connection = await aio_pika.connect_robust(
        host=settings.rabbitmq_host,
        port=settings.rabbitmq_port,
        login=settings.rabbitmq_username,
        password=settings.rabbitmq_password,
    )
    channel = await connection.channel()
    commands_exchange = await channel.get_exchange(settings.generation_commands_exchange)
    results_queue = await channel.get_queue(settings.generation_results_queue)
    requests_queue = await channel.get_queue(settings.generation_requests_queue)

    object_key: str | None = None
    try:
        generation_id = uuid4()
        command_id = uuid4()
        await commands_exchange.publish(
            aio_pika.Message(
                body=json.dumps(
                    {
                        "commandId": str(command_id),
                        "generationId": str(generation_id),
                        "prompt": "green square",
                        "type": "IMAGE",
                    }
                ).encode(),
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
            ),
            routing_key=settings.generation_generate_routing_key,
        )

        received: dict[str, dict] = {}
        for _ in range(2):
            message = await wait_for_message(results_queue)
            async with message.process():
                received[message.routing_key] = json.loads(message.body)

        assert received["generation.processing"]["status"] == "PROCESSING"
        completed = received["generation.completed"]
        assert completed["status"] == "COMPLETED"
        object_key = completed["asset"]["objectKey"]
        assert object_key == f"images/requests/{generation_id}/result.png"

        response = await asyncio.to_thread(
            storage._client.get_object,  # noqa: SLF001 - integration verification
            settings.minio_bucket,
            object_key,
        )
        try:
            image = Image.open(BytesIO(response.read()))
            assert image.size == (64, 64)
        finally:
            response.close()
            response.release_conn()
    finally:
        if object_key is not None:
            await asyncio.to_thread(
                storage._client.remove_object,  # noqa: SLF001
                settings.minio_bucket,
                object_key,
            )
        await messaging.close()
        await requests_queue.delete(if_unused=False, if_empty=False)
        await results_queue.delete(if_unused=False, if_empty=False)
        await connection.close()
