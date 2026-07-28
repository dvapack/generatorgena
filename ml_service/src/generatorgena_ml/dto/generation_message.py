"""RabbitMQ wire contracts shared with the Spring backend."""

from __future__ import annotations

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from generatorgena_ml.model import FailureCode, GenerationType


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
    generation_type: GenerationType = Field(alias="type")


class AssetPayload(MessageDto):
    object_key: str = Field(alias="objectKey")
    asset_type: Literal["IMAGE"] = Field(alias="assetType", default="IMAGE")
    content_type: Literal["image/png"] = Field(alias="contentType", default="image/png")
    size_bytes: int = Field(alias="sizeBytes", ge=1)
    width: int = Field(gt=0)
    height: int = Field(gt=0)
    duration: None = None


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
