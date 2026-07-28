# Docker

```shell
docker compose up --build --detach
```

For custom ports or credentials:

```shell
cp .env.example .env
docker compose up --build --detach
```

## Services

| Service | Default URL |
| --- | --- |
| Frontend | http://localhost:3000 |
| Spring backend | http://localhost:8080 |
| Backend health | http://localhost:8080/actuator/health/readiness |
| ML health | http://localhost:5001/health/ready |
| RabbitMQ UI | http://localhost:15672 |
| MinIO API | http://localhost:9000 |
| MinIO console | http://localhost:9001 |
| PostgreSQL | localhost:5432 |

The ML service is published on port `5001` because macOS commonly reserves
port `5000`. All host ports can be changed in `.env`.

## Verification

Inspect container health:

```shell
docker compose ps
docker compose logs --follow backend ml-service
```

Run backend tests:

```shell
docker compose --profile test run --build --rm backend-tests
```

Run ML linting, type checking, unit tests, and the RabbitMQ/MinIO integration
test:

```shell
docker compose --profile test run --build --rm ml-tests
```

## Shutdown

Keep local database and object-storage data:

```shell
docker compose down
```

Delete containers and all development data:

```shell
docker compose down --volumes
```

The default credentials are for local development only. Supply real secrets
through environment variables or a secrets manager for any deployed
environment.
