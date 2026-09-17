"""Environment-backed application configuration."""

from __future__ import annotations

from functools import lru_cache

from pydantic import BaseModel, ConfigDict, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class RabbitMqProperties(BaseModel):
    model_config = ConfigDict(frozen=True)

    host: str
    port: int
    username: str
    password: str
    commands_exchange: str
    events_exchange: str
    requests_queue: str
    results_queue: str
    generate_routing_key: str
    events_routing_pattern: str


class MinioProperties(BaseModel):
    model_config = ConfigDict(frozen=True)

    endpoint: str
    access_key: str
    secret_key: str
    bucket: str
    secure: bool
    region: str | None
    max_attempts: int


class ModelProperties(BaseModel):
    model_config = ConfigDict(frozen=True)

    checkpoint_path: str
    clip_model_name: str


class ApplicationSettings(BaseSettings):
    """Flat environment contract exposed as typed property groups."""

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

    @property
    def rabbitmq(self) -> RabbitMqProperties:
        return RabbitMqProperties(
            host=self.rabbitmq_host,
            port=self.rabbitmq_port,
            username=self.rabbitmq_username,
            password=self.rabbitmq_password,
            commands_exchange=self.generation_commands_exchange,
            events_exchange=self.generation_events_exchange,
            requests_queue=self.generation_requests_queue,
            results_queue=self.generation_results_queue,
            generate_routing_key=self.generation_generate_routing_key,
            events_routing_pattern=self.generation_events_routing_pattern,
        )

    @property
    def minio(self) -> MinioProperties:
        return MinioProperties(
            endpoint=self.minio_endpoint,
            access_key=self.minio_access_key,
            secret_key=self.minio_secret_key,
            bucket=self.minio_bucket,
            secure=self.minio_secure,
            region=self.minio_region,
            max_attempts=self.minio_max_attempts,
        )

    @property
    def model(self) -> ModelProperties:
        return ModelProperties(
            checkpoint_path=self.model_checkpoint_path,
            clip_model_name=self.clip_model_name,
        )


@lru_cache
def get_settings() -> ApplicationSettings:
    return ApplicationSettings()
