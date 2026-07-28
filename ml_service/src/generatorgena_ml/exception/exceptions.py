"""Exceptions crossing layer boundaries."""


class ImageGenerationError(RuntimeError):
    """The ML adapter could not generate an image."""


class AssetStorageError(RuntimeError):
    """The generated asset could not be persisted."""

    def __init__(self, message: str, *, retryable: bool):
        super().__init__(message)
        self.retryable = retryable


class EventPublicationError(RuntimeError):
    """A generation event could not be published."""


class MessageAcknowledgementError(RuntimeError):
    """An incoming RabbitMQ message could not be acknowledged."""
