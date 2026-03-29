"""Stream task manager for handling multiple concurrent streams."""
import threading
from typing import Dict, Optional

from .capture import StreamCapture


class StreamTaskManager:
    """
    Manages active stream inference tasks.
    Thread-safe singleton.
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
            self._tasks: Dict[str, StreamCapture] = {}
            self._task_lock = threading.RLock()
            self._initialized = True

    def start_task(
        self,
        task_id: str,
        stream_url: str,
        model_id: str,
        fps: int,
        callback_url: str
    ) -> bool:
        """
        Start a new stream task.

        Args:
            task_id: Unique task identifier
            stream_url: RTSP stream URL
            model_id: Model to use for inference
            fps: Target frames per second
            callback_url: URL to post inference results

        Returns:
            True if started successfully
        """
        with self._task_lock:
            if task_id in self._tasks:
                return False

            capture = StreamCapture(
                task_id=task_id,
                stream_url=stream_url,
                model_id=model_id,
                fps=fps,
                callback_url=callback_url
            )

            if capture.start():
                self._tasks[task_id] = capture
                return True
            return False

    def stop_task(self, task_id: str) -> bool:
        """
        Stop a running task.

        Args:
            task_id: Task identifier

        Returns:
            True if stopped, False if task not found
        """
        with self._task_lock:
            capture = self._tasks.pop(task_id, None)
            if capture:
                return capture.stop()
            return False

    def get_task(self, task_id: str) -> Optional[StreamCapture]:
        """Get a task by ID."""
        with self._task_lock:
            return self._tasks.get(task_id)

    def list_tasks(self) -> Dict[str, dict]:
        """
        List all active tasks.

        Returns:
            Dict of task_id to task info
        """
        with self._task_lock:
            return {
                task_id: {
                    'stream_url': capture.stream_url,
                    'model_id': capture.model_id,
                    'fps': capture.fps,
                    'callback_url': capture.callback_url,
                    'running': capture.is_running
                }
                for task_id, capture in self._tasks.items()
            }

    def stop_all(self):
        """Stop all running tasks."""
        with self._task_lock:
            for task_id in list(self._tasks.keys()):
                self.stop_task(task_id)
