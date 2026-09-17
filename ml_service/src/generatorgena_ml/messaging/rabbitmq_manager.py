"""RabbitMQ connection, topology, and consumer lifecycle."""

from __future__ import annotations

import asyncio
import logging
from collections.abc import Awaitable, Callable

import aio_pika
from aio_pika import DeliveryMode, ExchangeType, Message
from aio_pika.abc import (
    AbstractChannel,
    AbstractExchange,
    AbstractIncomingMessage,
    AbstractQueue,
    AbstractRobustConnection,
)

from generatorgena_ml.configuration.settings import RabbitMqProperties
from generatorgena_ml.exception import EventPublicationError

logger = logging.getLogger(__name__)
MessageHandler = Callable[[AbstractIncomingMessage], Awaitable[None]]


class RabbitMqManager:
    def __init__(self, properties: RabbitMqProperties):
        self._properties = properties
        self._connection: AbstractRobustConnection | None = None
        self._channel: AbstractChannel | None = None
        self._events_exchange: AbstractExchange | None = None
        self._requests_queue: AbstractQueue | None = None
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
        properties = self._properties
        self._connection = await aio_pika.connect_robust(
            host=properties.host,
            port=properties.port,
            login=properties.username,
            password=properties.password,
        )
        channel = await self._connection.channel(publisher_confirms=True)
        self._channel = channel
        await channel.set_qos(prefetch_count=1)

        commands_exchange = await channel.declare_exchange(
            properties.commands_exchange,
            ExchangeType.DIRECT,
            durable=True,
        )
        self._events_exchange = await channel.declare_exchange(
            properties.events_exchange,
            ExchangeType.TOPIC,
            durable=True,
        )
        self._requests_queue = await channel.declare_queue(
            properties.requests_queue,
            durable=True,
        )
        results_queue = await channel.declare_queue(
            properties.results_queue,
            durable=True,
        )
        await self._requests_queue.bind(
            commands_exchange,
            routing_key=properties.generate_routing_key,
        )
        await results_queue.bind(
            self._events_exchange,
            routing_key=properties.events_routing_pattern,
        )
        self._consumer_tag = await self._requests_queue.consume(
            self._tracked_handler(handler)
        )
        logger.info("Обработчик команд RabbitMQ запущен")

    def _tracked_handler(self, handler: MessageHandler) -> MessageHandler:
        async def tracked(message: AbstractIncomingMessage) -> None:
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

        return tracked

    async def publish(
        self,
        *,
        body: bytes,
        routing_key: str,
        event_id: str,
        command_id: str,
    ) -> None:
        if self._events_exchange is None:
            raise EventPublicationError(
                "Отправитель событий RabbitMQ не инициализирован"
            )
        try:
            await self._events_exchange.publish(
                Message(
                    body=body,
                    content_type="application/json",
                    content_encoding="utf-8",
                    delivery_mode=DeliveryMode.PERSISTENT,
                    message_id=event_id,
                    correlation_id=command_id,
                ),
                routing_key=routing_key,
            )
        except Exception as exception:
            raise EventPublicationError(
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
