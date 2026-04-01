"""Model lifecycle management with thread-safe model loading/unloading."""
import os
import logging
import threading
import time
from typing import Dict, Optional
from dataclasses import dataclass, field
from urllib.parse import urlparse, unquote

import requests
from ultralytics import YOLO

from config import Config

logger = logging.getLogger(__name__)

# Bypass system proxy for internal service communication
_NO_PROXY_SESSION = requests.Session()
_NO_PROXY_SESSION.trust_env = False


@dataclass
class ModelInfo:
    """Information about a loaded model."""
    model_id: str
    model_path: str
    device: str
    loaded_at: float
    download_url: str = ''
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

    def load_model(self, model_id: str, model_path: str = None,
                   device: str = 'cpu', download_url: str = None) -> bool:
        """
        Load a YOLO model into memory.

        If download_url is provided and the local file doesn't exist,
        the model file will be downloaded first.

        Args:
            model_id: Unique identifier for this model
            model_path: Path to the model file (.pt or .onnx), optional if download_url given
            device: 'cpu' or 'cuda'
            download_url: URL to download the model file from

        Returns:
            True if loaded successfully

        Raises:
            ValueError: If model file cannot be found or downloaded
            RuntimeError: If model fails to load
        """
        with self._model_lock:
            # Check if model already loaded
            if model_id in self._models:
                return True

            # Resolve model_path from download_url if not provided or file missing
            if download_url:
                if not model_path:
                    filename = self._extract_filename(download_url)
                    model_path = os.path.join(Config.MODEL_BASE_PATH, f'{model_id}_{filename}')
                if not os.path.exists(model_path):
                    self._download_file(download_url, model_path)

            # Validate path
            if not model_path or not os.path.exists(model_path):
                raise ValueError(f"Model file not found: {model_path}")

            try:
                model = YOLO(model_path)
                model.to(device)

                self._models[model_id] = model
                self._model_info[model_id] = ModelInfo(
                    model_id=model_id,
                    model_path=model_path,
                    device=device,
                    loaded_at=time.time(),
                    download_url=download_url or '',
                )

                if self._state_store:
                    self._state_store.add_model(model_id, model_path, device,
                                                download_url=download_url)

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
        If a model file is missing but a download_url is available,
        re-downloads the file before loading.
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
            download_url = entry.get('download_url')
            try:
                if not os.path.exists(model_path) and download_url:
                    logger.info('Model file missing, re-downloading: %s -> %s',
                                model_id, download_url)
                    self._download_file(download_url, model_path)

                if os.path.exists(model_path):
                    self.load_model(model_id, model_path, device,
                                    download_url=download_url)
                    logger.info('Restored model: %s', model_id)
                else:
                    logger.warning('Model file missing, skip restore: %s -> %s',
                                   model_id, model_path)
                    self._state_store.remove_model(model_id)
            except Exception as e:
                logger.error('Failed to restore model %s: %s', model_id, e)
                self._state_store.remove_model(model_id)

    @staticmethod
    def _download_file(url: str, dest_path: str):
        """Download a file from url to dest_path with streaming."""
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        logger.info('Downloading model: %s -> %s', url, dest_path)
        resp = _NO_PROXY_SESSION.get(url, stream=True, timeout=300)
        resp.raise_for_status()
        tmp_path = dest_path + '.tmp'
        try:
            with open(tmp_path, 'wb') as f:
                for chunk in resp.iter_content(chunk_size=8192):
                    f.write(chunk)
            os.replace(tmp_path, dest_path)
            logger.info('Download complete: %s', dest_path)
        except Exception:
            if os.path.exists(tmp_path):
                os.unlink(tmp_path)
            raise

    @staticmethod
    def _extract_filename(url: str) -> str:
        """Extract filename from a URL."""
        path = urlparse(url).path
        filename = unquote(os.path.basename(path))
        return filename if filename else 'model.pt'
