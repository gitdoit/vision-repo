"""Model lifecycle management with thread-safe model loading/unloading."""
import os
import logging
import threading
from typing import Dict, Optional
from dataclasses import dataclass

from ultralytics import YOLO

logger = logging.getLogger(__name__)


@dataclass
class ModelInfo:
    """Information about a loaded model."""
    model_id: str
    model_path: str
    device: str
    loaded_at: float
    status: str = 'loaded'


class ModelManager:
    """
    Thread-safe manager for YOLO models.
    Handles loading, unloading, and querying of model instances.
    """
    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if not hasattr(self, '_initialized'):
            self._models: Dict[str, YOLO] = {}
            self._model_info: Dict[str, ModelInfo] = {}
            self._model_lock = threading.RLock()
            self._state_store = None
            self._initialized = True

    def set_state_store(self, state_store):
        """Inject StateStore for persistence. Call before loading models."""
        self._state_store = state_store

    def load_model(self, model_id: str, model_path: str, device: str = 'cpu') -> bool:
        """
        Load a YOLO model into memory.

        Args:
            model_id: Unique identifier for this model
            model_path: Path to the model file (.pt or .onnx)
            device: 'cpu' or 'cuda'

        Returns:
            True if loaded successfully

        Raises:
            ValueError: If model_path doesn't exist
            RuntimeError: If model fails to load
        """
        with self._model_lock:
            # Check if model already loaded
            if model_id in self._models:
                return True

            # Validate path
            if not os.path.exists(model_path):
                raise ValueError(f"Model file not found: {model_path}")

            try:
                import time
                model = YOLO(model_path)
                model.to(device)

                self._models[model_id] = model
                self._model_info[model_id] = ModelInfo(
                    model_id=model_id,
                    model_path=model_path,
                    device=device,
                    loaded_at=time.time()
                )

                if self._state_store:
                    self._state_store.add_model(model_id, model_path, device)

                return True
            except Exception as e:
                raise RuntimeError(f"Failed to load model {model_id}: {e}")

    def unload_model(self, model_id: str) -> bool:
        """
        Unload a model from memory.

        Args:
            model_id: Model identifier to unload

        Returns:
            True if unloaded, False if model not found
        """
        with self._model_lock:
            if model_id not in self._models:
                return False

            del self._models[model_id]
            del self._model_info[model_id]

            if self._state_store:
                self._state_store.remove_model(model_id)

            return True

    def get_model(self, model_id: str) -> Optional[YOLO]:
        """
        Get a loaded model instance.

        Args:
            model_id: Model identifier

        Returns:
            YOLO model instance or None if not loaded
        """
        with self._model_lock:
            return self._models.get(model_id)

    def list_models(self) -> Dict[str, ModelInfo]:
        """
        List all loaded models.

        Returns:
            Dictionary of model_id to ModelInfo
        """
        with self._model_lock:
            return dict(self._model_info)

    def is_loaded(self, model_id: str) -> bool:
        """Check if a model is currently loaded."""
        with self._model_lock:
            return model_id in self._models

    def restore_models(self):
        """
        Restore previously loaded models from state store.
        Called on startup to recover from restart.
        """
        if not self._state_store:
            return

        models = self._state_store.get_loaded_models()
        if not models:
            logger.info('No models to restore')
            return

        logger.info('Restoring %d model(s) from state', len(models))
        for entry in models:
            model_id = entry.get('model_id')
            model_path = entry.get('model_path')
            device = entry.get('device', 'cpu')
            try:
                if os.path.exists(model_path):
                    self.load_model(model_id, model_path, device)
                    logger.info('Restored model: %s', model_id)
                else:
                    logger.warning('Model file missing, skip restore: %s -> %s', model_id, model_path)
                    self._state_store.remove_model(model_id)
            except Exception as e:
                logger.error('Failed to restore model %s: %s', model_id, e)
                self._state_store.remove_model(model_id)
