"""Framework-independent generation domain models."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import UTC, datetime
from enum import StrEnum
from uuid import UUID, uuid4


def utc_now() -> datetime:
    return datetime.now(UTC)


class FailureCode(StrEnum):
    GENERATION_ERROR = "GENERATION_ERROR"
    STORAGE_ERROR = "STORAGE_ERROR"


@dataclass(frozen=True, slots=True)
class GenerationCommand:
    command_id: UUID
    generation_id: UUID
    prompt: str
    status: str = "QUEUED"


@dataclass(frozen=True, slots=True)
class GeneratedImage:
    data: bytes
    content_type: str
    size_bytes: int
    width: int
    height: int


@dataclass(frozen=True, slots=True)
class GeneratedAsset:
    object_key: str
    size_bytes: int
    content_type: str


@dataclass(frozen=True, slots=True)
class ProcessingEvent:
    command_id: UUID
    generation_id: UUID
    event_id: UUID = field(default_factory=uuid4)
    occurred_at: datetime = field(default_factory=utc_now)
    status: str = field(default="PROCESSING", init=False)


@dataclass(frozen=True, slots=True)
class CompletedEvent:
    command_id: UUID
    generation_id: UUID
    asset: GeneratedAsset
    event_id: UUID = field(default_factory=uuid4)
    occurred_at: datetime = field(default_factory=utc_now)
    status: str = field(default="COMPLETED", init=False)


@dataclass(frozen=True, slots=True)
class FailedEvent:
    command_id: UUID
    generation_id: UUID
    code: FailureCode
    message: str
    retryable: bool
    event_id: UUID = field(default_factory=uuid4)
    occurred_at: datetime = field(default_factory=utc_now)
    status: str = field(default="FAILED", init=False)


GenerationEvent = ProcessingEvent | CompletedEvent | FailedEvent
