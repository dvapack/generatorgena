import pytest
from fastapi import HTTPException

from generatorgena_ml.controller import HealthController


class Probe:
    def __init__(self, ready: bool):
        self.ready = ready

    async def is_ready(self) -> bool:
        return self.ready


async def test_liveness_is_always_up() -> None:
    controller = HealthController(Probe(False))

    assert await controller.liveness() == {"status": "UP"}


async def test_readiness_returns_503_when_dependencies_are_not_ready() -> None:
    controller = HealthController(Probe(False))

    with pytest.raises(HTTPException) as error:
        await controller.readiness()

    assert error.value.status_code == 503
