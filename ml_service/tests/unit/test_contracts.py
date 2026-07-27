from uuid import uuid4

import pytest
from pydantic import ValidationError

from generatorgena_ml.domain.contracts import (
    AssetPayload,
    CompletedEvent,
    GenerateContentCommand,
    GenerationType,
)


def test_command_uses_backend_camel_case_contract() -> None:
    command_id = uuid4()
    generation_id = uuid4()

    command = GenerateContentCommand.model_validate(
        {
            "commandId": str(command_id),
            "generationId": str(generation_id),
            "prompt": "flowers in a vase",
            "type": "IMAGE",
        }
    )

    assert command.command_id == command_id
    assert command.generation_id == generation_id
    assert command.type is GenerationType.IMAGE


def test_command_rejects_unknown_fields() -> None:
    with pytest.raises(ValidationError):
        GenerateContentCommand.model_validate(
            {
                "commandId": str(uuid4()),
                "generationId": str(uuid4()),
                "prompt": "flowers",
                "type": "IMAGE",
                "schemaVersion": 1,
            }
        )


def test_completed_event_serializes_expected_asset_shape() -> None:
    event = CompletedEvent(
        commandId=uuid4(),
        generationId=uuid4(),
        asset=AssetPayload(
            objectKey="images/requests/id/result.png",
            sizeBytes=123,
            width=64,
            height=64,
        ),
    )

    payload = event.model_dump(mode="json", by_alias=True)

    assert payload["status"] == "COMPLETED"
    assert payload["asset"] == {
        "objectKey": "images/requests/id/result.png",
        "assetType": "IMAGE",
        "contentType": "image/png",
        "sizeBytes": 123,
        "width": 64,
        "height": 64,
        "duration": None,
    }
    assert "schemaVersion" not in payload
