import ast
from pathlib import Path

SOURCE_ROOT = Path(__file__).parents[2] / "src" / "generatorgena_ml"
FRAMEWORK_MODULES = {
    "aio_pika",
    "fastapi",
    "minio",
    "pydantic",
    "torch",
    "torchvision",
    "transformers",
}


def imported_root_modules(path: Path) -> set[str]:
    tree = ast.parse(path.read_text(encoding="utf-8"))
    imported: set[str] = set()
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            imported.update(
                alias.name.split(".", maxsplit=1)[0] for alias in node.names
            )
        elif isinstance(node, ast.ImportFrom) and node.module:
            imported.add(node.module.split(".", maxsplit=1)[0])
    return imported


def test_domain_and_service_layers_do_not_import_frameworks() -> None:
    protected_files = [
        *SOURCE_ROOT.joinpath("model").glob("*.py"),
        *SOURCE_ROOT.joinpath("service").glob("*.py"),
    ]

    violations = {
        path.relative_to(SOURCE_ROOT): imported_root_modules(path) & FRAMEWORK_MODULES
        for path in protected_files
        if imported_root_modules(path) & FRAMEWORK_MODULES
    }

    assert violations == {}
