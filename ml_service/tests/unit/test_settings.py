from generatorgena_ml.configuration.settings import ApplicationSettings


def test_flat_environment_contract_is_exposed_as_typed_groups() -> None:
    settings = ApplicationSettings(
        rabbitmq_host="rabbit",
        minio_access_key="worker",
        minio_secret_key="worker-secret",
        model_checkpoint_path="/models/generator.pt",
    )

    assert settings.rabbitmq.host == "rabbit"
    assert settings.minio.access_key == "worker"
    assert settings.model.checkpoint_path == "/models/generator.pt"
