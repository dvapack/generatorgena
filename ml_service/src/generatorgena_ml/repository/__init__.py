"""Generated asset persistence port and adapters."""

from generatorgena_ml.repository.generated_asset_repository import (
    GeneratedAssetRepository,
)
from generatorgena_ml.repository.minio_generated_asset_repository import (
    MinioGeneratedAssetRepository,
)

__all__ = ["GeneratedAssetRepository", "MinioGeneratedAssetRepository"]
