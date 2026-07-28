FROM python:3.11-slim AS dependencies

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1 \
    HF_HOME=/opt/huggingface

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

ARG CLIP_MODEL_NAME=openai/clip-vit-base-patch32
RUN python -c "from transformers import CLIPTextModel, CLIPTokenizer; name='${CLIP_MODEL_NAME}'; CLIPTokenizer.from_pretrained(name); CLIPTextModel.from_pretrained(name)"

FROM dependencies AS runtime

ENV PYTHONPATH=/app/src

RUN groupadd --system app && \
    useradd --system --gid app --home /app app && \
    chown app:app /app

COPY --chown=app:app src ./src
COPY --chown=app:app Generator_70.pt ./
RUN chmod -R a+rX "${HF_HOME}"

USER app

EXPOSE 5000

HEALTHCHECK --interval=15s --timeout=5s --start-period=5m --retries=5 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://localhost:5000/health/ready', timeout=3)"]

CMD ["uvicorn", "generatorgena_ml.main:app", "--host", "0.0.0.0", "--port", "5000"]

FROM runtime AS test

USER root
COPY requirements-test.txt .
RUN pip install --no-cache-dir -r requirements-test.txt
COPY --chown=app:app pytest.ini .
COPY --chown=app:app pyproject.toml .
COPY --chown=app:app tests ./tests
USER app

CMD ["sh", "-c", "ruff check src tests && ruff format --check src tests && mypy src && pytest -q"]
