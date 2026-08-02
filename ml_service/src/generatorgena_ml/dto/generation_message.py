"""RabbitMQ wire contracts shared with the Spring backend."""

from __future__ import annotations

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from generatorgena_ml.model import FailureCode


class MessageDto(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        serialize_by_alias=True,
        extra="forbid",
    )


class GenerateContentCommandDto(MessageDto):
    command_id: UUID = Field(alias="commandId")
    generation_id: UUID = Field(alias="generationId")
    prompt: str = Field(min_length=1)
    status: Literal["QUEUED"]


class AssetPayload(MessageDto):
    object_key: str = Field(alias="objectKey")
    content_type: str = Field(alias="contentType", min_length=1)
    size_bytes: int = Field(alias="sizeBytes", ge=1)


class ErrorPayload(MessageDto):
    code: FailureCode
    message: str
    retryable: bool


class ProcessingEventDto(MessageDto):
    event_id: UUID = Field(alias="eventId")
    command_id: UUID = Field(alias="commandId")
    generation_id: UUID = Field(alias="generationId")
    status: Literal["PROCESSING"]
    occurred_at: datetime = Field(alias="occurredAt")


class CompletedEventDto(MessageDto):
    event_id: UUID = Field(alias="eventId")
    command_id: UUID = Field(alias="commandId")
    generation_id: UUID = Field(alias="generationId")
    status: Literal["COMPLETED"]
    occurred_at: datetime = Field(alias="occurredAt")
    asset: AssetPayload


class FailedEventDto(MessageDto):
    event_id: UUID = Field(alias="eventId")
    command_id: UUID = Field(alias="commandId")
    generation_id: UUID = Field(alias="generationId")
    status: Literal["FAILED"]
    occurred_at: datetime = Field(alias="occurredAt")
    error: ErrorPayload


GenerationEventDto = ProcessingEventDto | CompletedEventDto | FailedEventDto
