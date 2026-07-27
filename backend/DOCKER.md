# Local Docker stack

Create the local environment file once:

```shell
cp backend/.env.example backend/.env
```

Build and start the application:

```shell
docker compose --env-file backend/.env -f backend/compose.yaml up --build -d
```

The ML health API is published on `http://localhost:5001` by default because
macOS commonly reserves port `5000`. Set `ML_SERVICE_PORT` to override it.

Run only the ML test container and its dependencies:

```shell
docker compose --env-file backend/.env -f backend/compose.yaml \
  --profile test up --build --abort-on-container-exit ml-tests
```

Stop containers while keeping development data:

```shell
docker compose --env-file backend/.env -f backend/compose.yaml down
```

Add `--volumes` only when PostgreSQL, RabbitMQ and MinIO data should be reset.
