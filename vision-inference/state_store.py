"""Local state persistence for inference service restart recovery."""
import json
import os
import tempfile
import threading
import logging

logger = logging.getLogger(__name__)


class StateStore:
    """
    Persists node state to a JSON file on disk.
    Ensures the inference service can restore loaded models after restart.
    Uses atomic write (write to temp file then rename) to prevent corruption.
    """

    def __init__(self, file_path: str):
        self._file_path = file_path
        self._lock = threading.Lock()
        self._state = self._load_from_disk()

    def _load_from_disk(self) -> dict:
        """Load state from JSON file. Returns default if file doesn't exist."""
        if not os.path.exists(self._file_path):
            return {'node_id': None, 'loaded_models': []}
        try:
            with open(self._file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            logger.info('State loaded from %s', self._file_path)
            return data
        except Exception as e:
            logger.warning('Failed to load state file %s: %s', self._file_path, e)
            return {'node_id': None, 'loaded_models': []}

    def _save_to_disk(self):
        """Atomically write state to disk."""
        try:
            os.makedirs(os.path.dirname(self._file_path), exist_ok=True)
            # Write to temp file first, then rename for atomicity
            fd, tmp_path = tempfile.mkstemp(
                dir=os.path.dirname(self._file_path),
                suffix='.tmp'
            )
            try:
                with os.fdopen(fd, 'w', encoding='utf-8') as f:
                    json.dump(self._state, f, indent=2, ensure_ascii=False)
                os.replace(tmp_path, self._file_path)
            except Exception:
                # Clean up temp file on failure
                try:
                    os.unlink(tmp_path)
                except OSError:
                    pass
                raise
        except Exception as e:
            logger.error('Failed to save state to %s: %s', self._file_path, e)

    def load_state(self) -> dict:
        """Get current state."""
        with self._lock:
            return dict(self._state)

    def save_node_id(self, node_id: str):
        """Persist the assigned node ID."""
        with self._lock:
            self._state['node_id'] = node_id
            self._save_to_disk()

    def get_node_id(self) -> str | None:
        """Get persisted node ID."""
        with self._lock:
            return self._state.get('node_id')

    def add_model(self, model_id: str, model_path: str, device: str):
        """Add a model record and persist immediately."""
        with self._lock:
            # Remove existing entry with same model_id
            self._state['loaded_models'] = [
                m for m in self._state.get('loaded_models', [])
                if m.get('model_id') != model_id
            ]
            self._state['loaded_models'].append({
                'model_id': model_id,
                'model_path': model_path,
                'device': device
            })
            self._save_to_disk()

    def remove_model(self, model_id: str):
        """Remove a model record and persist immediately."""
        with self._lock:
            self._state['loaded_models'] = [
                m for m in self._state.get('loaded_models', [])
                if m.get('model_id') != model_id
            ]
            self._save_to_disk()

    def get_loaded_models(self) -> list:
        """Get list of persisted loaded models."""
        with self._lock:
            return list(self._state.get('loaded_models', []))
