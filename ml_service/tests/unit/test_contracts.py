from uuid import uuid4

import pytest
from pydantic import ValidationError

from generatorgena_ml.dto import GenerateContentCommandDto
from generatorgena_ml.mapper import GenerationMessageMapper
from generatorgena_ml.model import (
    CompletedEvent,
    GeneratedAsset,
)


def test_command_uses_backend_camel_case_contract() -> None:
    command_id = uuid4()
    generation_id = uuid4()

    dto = GenerateContentCommandDto.model_validate(
        {
            "commandId": str(command_id),
            "generationId": str(generation_id),
            "prompt": "flowers in a vase",
            "status": "QUEUED",
        }
    )
    command = GenerationMessageMapper.to_command(dto)

    assert command.command_id == command_id
    assert command.generation_id == generation_id
    assert command.prompt == "flowers in a vase"
    assert command.status == "QUEUED"


def test_command_rejects_unknown_fields() -> None:
    with pytest.raises(ValidationError):
        GenerateContentCommandDto.model_validate(
            {
                "commandId": str(uuid4()),
                "generationId": str(uuid4()),
                "prompt": "flowers",
                "status": "QUEUED",
                "schemaVersion": 1,
            }
        )


def test_completed_event_serializes_expected_asset_shape() -> None:
    event = CompletedEvent(
        command_id=uuid4(),
        generation_id=uuid4(),
        asset=GeneratedAsset(
            object_key="images/requests/id/result.png",
            size_bytes=123,
            content_type="image/png",
        ),
    )

    dto = GenerationMessageMapper.to_event_dto(event)
    payload = dto.model_dump(mode="json", by_alias=True)

    assert payload["status"] == "COMPLETED"
    assert payload["asset"] == {
        "objectKey": "images/requests/id/result.png",
        "contentType": "image/png",
        "sizeBytes": 123,
    }
    assert "schemaVersion" not in payload
