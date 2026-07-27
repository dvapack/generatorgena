from __future__ import annotations

import asyncio
import json
import logging
from collections.abc import Awaitable, Callable

import aio_pika
from aio_pika import DeliveryMode, ExchangeType, IncomingMessage, Message
from aio_pika.abc import (
    AbstractRobustChannel,
    AbstractRobustConnection,
    AbstractRobustExchange,
    AbstractRobustQueue,
)

from generatorgena_ml.config import Settings
from generatorgena_ml.domain.contracts import GenerationEvent
from generatorgena_ml.domain.errors import MessagingError


logger = logging.getLogger(__name__)
MessageHandler = Callable[[IncomingMessage], Awaitable[None]]


class RabbitMessaging:
    def __init__(self, settings: Settings):
        self._settings = settings
        self._connection: AbstractRobustConnection | None = None
        self._channel: AbstractRobustChannel | None = None
        self._events_exchange: AbstractRobustExchange | None = None
        self._requests_queue: AbstractRobustQueue | None = None
        self._consumer_tag: str | None = None
        self._active_handlers: set[asyncio.Task[None]] = set()
        self._handlers_idle = asyncio.Event()
        self._handlers_idle.set()

    @property
    def is_ready(self) -> bool:
        return (
            self._connection is not None
            and not self._connection.is_closed
            and self._channel is not None
            and not self._channel.is_closed
            and self._consumer_tag is not None
        )

    async def start(self, handler: MessageHandler) -> None:
        settings = self._settings
        self._connection = await aio_pika.connect_robust(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            login=settings.rabbitmq_username,
            password=settings.rabbitmq_password,
        )
        self._channel = await self._connection.channel(publisher_confirms=True)
        await self._channel.set_qos(prefetch_count=1)

        commands_exchange = await self._channel.declare_exchange(
            settings.generation_commands_exchange,
            ExchangeType.DIRECT,
            durable=True,
        )
        self._events_exchange = await self._channel.declare_exchange(
            settings.generation_events_exchange,
            ExchangeType.TOPIC,
            durable=True,
        )
        self._requests_queue = await self._channel.declare_queue(
            settings.generation_requests_queue,
            durable=True,
        )
        results_queue = await self._channel.declare_queue(
            settings.generation_results_queue,
            durable=True,
        )
        await self._requests_queue.bind(
            commands_exchange,
            routing_key=settings.generation_generate_routing_key,
        )
        await results_queue.bind(
            self._events_exchange,
            routing_key=settings.generation_events_routing_pattern,
        )
        async def tracked_handler(message: IncomingMessage) -> None:
            await self._run_handler(handler, message)

        self._consumer_tag = await self._requests_queue.consume(tracked_handler)
        logger.info("Обработчик команд RabbitMQ запущен")

    async def _run_handler(
        self,
        handler: MessageHandler,
        message: IncomingMessage,
    ) -> None:
        task = asyncio.current_task()
        if task is not None:
            self._active_handlers.add(task)
            self._handlers_idle.clear()
        try:
            await handler(message)
        finally:
            if task is not None:
                self._active_handlers.discard(task)
            if not self._active_handlers:
                self._handlers_idle.set()

    async def publish_event(self, event: GenerationEvent, routing_key: str) -> None:
        if self._events_exchange is None:
            raise RuntimeError("Отправитель событий RabbitMQ не инициализирован")
        payload = event.model_dump(
            mode="json",
            by_alias=True,
            exclude_none=False,
        )
        try:
            await self._events_exchange.publish(
                Message(
                    body=json.dumps(payload, separators=(",", ":")).encode(),
                    content_type="application/json",
                    content_encoding="utf-8",
                    delivery_mode=DeliveryMode.PERSISTENT,
                    message_id=str(event.event_id),
                    correlation_id=str(event.command_id),
                ),
                routing_key=routing_key,
            )
        except Exception as exception:
            raise MessagingError(
                "Не удалось опубликовать событие в RabbitMQ"
            ) from exception

    async def close(self) -> None:
        if self._requests_queue is not None and self._consumer_tag is not None:
            try:
                await self._requests_queue.cancel(self._consumer_tag)
            except Exception:
                logger.warning(
                    "Не удалось корректно остановить обработчик команд RabbitMQ",
                    exc_info=True,
                )
        self._consumer_tag = None
        await self._handlers_idle.wait()
        if self._connection is not None and not self._connection.is_closed:
            await self._connection.close()
        self._connection = None
        self._channel = None
        self._events_exchange = None
        self._requests_queue = None
