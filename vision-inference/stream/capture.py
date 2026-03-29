"""RTSP stream capture with OpenCV."""
import threading
import time
from typing import Optional, Callable, Dict, Any
import queue

import cv2
import requests

from ..inference.engine import InferenceEngine


class StreamCapture:
    """
    Captures frames from RTSP stream and runs inference.
    Runs in a background thread and posts results via callback.
    """

    def __init__(
        self,
        task_id: str,
        stream_url: str,
        model_id: str,
        fps: int,
        callback_url: str,
        inference_engine: Optional[InferenceEngine] = None
    ):
        self.task_id = task_id
        self.stream_url = stream_url
        self.model_id = model_id
        self.fps = fps
        self.callback_url = callback_url
        self.inference_engine = inference_engine or InferenceEngine()

        self._running = False
        self._thread: Optional[threading.Thread] = None
        self._capture: Optional[cv2.VideoCapture] = None

    def start(self) -> bool:
        """
        Start capturing and processing frames.

        Returns:
            True if started successfully
        """
        if self._running:
            return False

        self._running = True
        self._thread = threading.Thread(target=self._capture_loop, daemon=True)
        self._thread.start()
        return True

    def stop(self) -> bool:
        """
        Stop capturing.

        Returns:
            True if stopped successfully
        """
        if not self._running:
            return False

        self._running = False
        if self._thread:
            self._thread.join(timeout=5)
        self._cleanup()
        return True

    def _cleanup(self):
        """Release resources."""
        if self._capture:
            self._capture.release()
            self._capture = None

    def _capture_loop(self):
        """Main capture loop running in background thread."""
        # Open RTSP stream
        self._capture = cv2.VideoCapture(self.stream_url)

        if not self._capture.isOpened():
            print(f"[{self.task_id}] Failed to open stream: {self.stream_url}")
            self._running = False
            return

        frame_interval = 1.0 / self.fps
        last_frame_time = 0

        try:
            while self._running:
                current_time = time.time()

                # Throttle to target FPS
                if current_time - last_frame_time < frame_interval:
                    time.sleep(0.01)
                    continue

                ret, frame = self._capture.read()
                if not ret:
                    print(f"[{self.task_id}] Failed to read frame, reconnecting...")
                    self._capture.release()
                    time.sleep(1)
                    self._capture = cv2.VideoCapture(self.stream_url)
                    continue

                last_frame_time = current_time

                # Run inference
                try:
                    detections, inference_time = self.inference_engine.predict(
                        image_source=frame,
                        model_id=self.model_id
                    )

                    # Send callback
                    self._send_callback(detections, inference_time)

                except Exception as e:
                    print(f"[{self.task_id}] Inference error: {e}")

        except Exception as e:
            print(f"[{self.task_id}] Capture loop error: {e}")
        finally:
            self._cleanup()

    def _send_callback(self, detections, inference_time: float):
        """Send inference results to callback URL."""
        payload = {
            'task_id': self.task_id,
            'stream_url': self.stream_url,
            'model_id': self.model_id,
            'timestamp': int(time.time() * 1000),
            'objects': [d.to_dict() for d in detections],
            'inference_time_ms': inference_time
        }

        try:
            response = requests.post(
                self.callback_url,
                json=payload,
                timeout=5
            )
            if response.status_code >= 400:
                print(f"[{self.task_id}] Callback failed: {response.status_code}")
        except requests.RequestException as e:
            print(f"[{self.task_id}] Callback error: {e}")

    @property
    def is_running(self) -> bool:
        """Check if capture is currently running."""
        return self._running
