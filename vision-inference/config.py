"""
Configuration module for vision-inference service.
Reads settings from environment variables with sensible defaults.
"""
import os


class Config:
    """Application configuration."""

    # Model settings
    MODEL_BASE_PATH = os.getenv('MODEL_BASE_PATH', '/data/vision/models')

    # Device: 'cpu' or 'cuda'
    DEVICE = os.getenv('DEVICE', 'cpu')

    # Server settings
    HOST = os.getenv('HOST', '0.0.0.0')
    PORT = int(os.getenv('PORT', '5000'))

    # Log level: DEBUG, INFO, WARNING, ERROR
    LOG_LEVEL = os.getenv('LOG_LEVEL', 'INFO')

    # Inference settings
    DEFAULT_CONFIDENCE_THRESHOLD = float(os.getenv('DEFAULT_CONFIDENCE_THRESHOLD', '0.5'))
    INFERENCE_TIMEOUT = int(os.getenv('INFERENCE_TIMEOUT', '30'))

    # Stream settings
    DEFAULT_STREAM_FPS = int(os.getenv('DEFAULT_STREAM_FPS', '5'))
    MAX_STREAM_TASKS = int(os.getenv('MAX_STREAM_TASKS', '10'))

    @classmethod
    def validate(cls):
        """Validate configuration and raise errors if invalid."""
        if cls.DEVICE not in ('cpu', 'cuda'):
            raise ValueError(f"Invalid DEVICE: {cls.DEVICE}. Must be 'cpu' or 'cuda'")

        if cls.DEFAULT_CONFIDENCE_THRESHOLD < 0 or cls.DEFAULT_CONFIDENCE_THRESHOLD > 1:
            raise ValueError("DEFAULT_CONFIDENCE_THRESHOLD must be between 0 and 1")
