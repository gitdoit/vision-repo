"""YOLO inference engine."""
import time
from typing import List, Dict, Union, Optional

import numpy as np
from ultralytics import YOLO

from .model_manager import ModelManager
from .preprocess import load_image


class InferenceResult:
    """Single detection result."""

    def __init__(self, label: str, confidence: float, bbox: List[float]):
        self.label = label
        self.confidence = confidence
        self.bbox = bbox  # [x1, y1, x2, y2] in absolute coordinates

    def to_dict(self) -> Dict:
        """Convert to dictionary for JSON serialization."""
        return {
            'label': self.label,
            'confidence': float(self.confidence),
            'bbox': [float(x) for x in self.bbox]
        }


class InferenceEngine:
    """
    YOLO inference engine wrapper.
    Handles model retrieval and prediction execution.
    """

    def __init__(self):
        self.model_manager = ModelManager()

    def predict(
        self,
        image_source: Union[str, bytes],
        model_id: str,
        confidence_threshold: float = 0.5
    ) -> tuple[List[InferenceResult], float]:
        """
        Run inference on an image.

        Args:
            image_source: Image URL, local path, or bytes
            model_id: ID of the model to use
            confidence_threshold: Minimum confidence for detections

        Returns:
            Tuple of (list of InferenceResult, inference_time_ms)

        Raises:
            ValueError: If model not loaded or image fails to load
        """
        model = self.model_manager.get_model(model_id)
        if model is None:
            raise ValueError(f"Model '{model_id}' not loaded. Call /models/load first.")

        # Load and preprocess image
        image = load_image(image_source)

        # Run inference
        start_time = time.time()
        results = model.predict(
            image,
            conf=confidence_threshold,
            verbose=False
        )
        inference_time_ms = (time.time() - start_time) * 1000

        # Parse results
        detections = []
        if results and len(results) > 0:
            result = results[0]
            if hasattr(result, 'boxes') and result.boxes is not None:
                boxes = result.boxes
                for i in range(len(boxes)):
                    box = boxes.xyxy[i].cpu().numpy()
                    conf = float(boxes.conf[i].cpu().numpy())
                    cls_id = int(boxes.cls[i].cpu().numpy())
                    label = model.names.get(cls_id, f'class_{cls_id}')

                    detections.append(InferenceResult(
                        label=label,
                        confidence=conf,
                        bbox=box.tolist()
                    ))

        return detections, inference_time_ms
