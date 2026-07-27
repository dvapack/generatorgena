"""RabbitMQ command and event contracts."""

from __future__ import annotations

from datetime import datetime, timezone
from enum import StrEnum
from typing import Literal
from uuid import UUID, uuid4

from pydantic import BaseModel, ConfigDict, Field


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class ContractModel(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        serialize_by_alias=True,
        extra="forbid",
    )


class GenerationType(StrEnum):
    IMAGE = "IMAGE"
    VIDEO = "VIDEO"
    AUDIO = "AUDIO"


class GenerateContentCommand(ContractModel):
    command_id: UUID = Field(alias="commandId")
    generation_id: UUID = Field(alias="generationId")
    prompt: str = Field(min_length=1)
    type: GenerationType


class AssetPayload(ContractModel):
    object_key: str = Field(alias="objectKey")
    asset_type: Literal["IMAGE"] = Field(alias="assetType", default="IMAGE")
    content_type: Literal["image/png"] = Field(alias="contentType", default="image/png")
    size_bytes: int = Field(alias="sizeBytes", ge=1)
    width: int = Field(gt=0)
    height: int = Field(gt=0)
    duration: None = None


class ErrorPayload(ContractModel):
    code: Literal["UNSUPPORTED_TYPE", "GENERATION_ERROR", "STORAGE_ERROR"]
    message: str
    retryable: bool


class ProcessingEvent(ContractModel):
    event_id: UUID = Field(alias="eventId", default_factory=uuid4)
    command_id: UUID = Field(alias="commandId")
    generation_id: UUID = Field(alias="generationId")
    status: Literal["PROCESSING"] = "PROCESSING"
    occurred_at: datetime = Field(alias="occurredAt", default_factory=utc_now)


class CompletedEvent(ContractModel):
    event_id: UUID = Field(alias="eventId", default_factory=uuid4)
    command_id: UUID = Field(alias="commandId")
    generation_id: UUID = Field(alias="generationId")
    status: Literal["COMPLETED"] = "COMPLETED"
    occurred_at: datetime = Field(alias="occurredAt", default_factory=utc_now)
    asset: AssetPayload


class FailedEvent(ContractModel):
    event_id: UUID = Field(alias="eventId", default_factory=uuid4)
    command_id: UUID = Field(alias="commandId")
    generation_id: UUID = Field(alias="generationId")
    status: Literal["FAILED"] = "FAILED"
    occurred_at: datetime = Field(alias="occurredAt", default_factory=utc_now)
    error: ErrorPayload


GenerationEvent = ProcessingEvent | CompletedEvent | FailedEvent
