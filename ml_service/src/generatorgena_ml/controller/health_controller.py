"""FastAPI health endpoints."""

from typing import Protocol

from fastapi import APIRouter, HTTPException, status


class ReadinessProbe(Protocol):
    async def is_ready(self) -> bool: ...


class HealthController:
    def __init__(self, readiness_probe: ReadinessProbe):
        self._readiness_probe = readiness_probe
        self.router = APIRouter()
        self.router.add_api_route(
            "/health/live",
            self.liveness,
            methods=["GET"],
        )
        self.router.add_api_route(
            "/health/ready",
            self.readiness,
            methods=["GET"],
        )

    async def liveness(self) -> dict[str, str]:
        return {"status": "UP"}

    async def readiness(self) -> dict[str, str]:
        if not await self._readiness_probe.is_ready():
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="ML-сервис не готов к работе",
            )
        return {"status": "UP"}
