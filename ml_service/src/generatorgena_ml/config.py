"""Environment-backed application configuration."""

from __future__ import annotations

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_username: str = "myuser"
    rabbitmq_password: str = "secret"

    generation_commands_exchange: str = "generation.commands"
    generation_events_exchange: str = "generation.events"
    generation_requests_queue: str = "generation.requests"
    generation_results_queue: str = "generation.results"
    generation_generate_routing_key: str = "generation.generate"
    generation_events_routing_pattern: str = "generation.*"

    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = Field(min_length=3)
    minio_secret_key: str = Field(min_length=8)
    minio_bucket: str = "generated-assets"
    minio_secure: bool = False
    minio_region: str | None = None
    minio_max_attempts: int = Field(default=3, ge=1, le=10)

    model_checkpoint_path: str = "./Generator_70.pt"
    clip_model_name: str = "openai/clip-vit-base-patch32"


@lru_cache
def get_settings() -> Settings:
    return Settings()
