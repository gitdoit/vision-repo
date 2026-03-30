"""YOLO inference engine."""
import base64
import io
import time
from typing import List, Dict, Union, Optional

import numpy as np
from ultralytics import YOLO

from .model_manager import ModelManager
from .preprocess import load_image


class InferenceResult:
    """Single detection result (detect / segment / pose)."""

    def __init__(self, label: str, confidence: float, bbox: List[float],
                 mask_png_b64: Optional[str] = None,
                 keypoints: Optional[List[List[float]]] = None):
        self.label = label
        self.confidence = confidence
        self.bbox = bbox  # [x1, y1, x2, y2] in absolute coordinates
        self.mask_png_b64 = mask_png_b64  # Base64 encoded PNG mask (segment only)
        self.keypoints = keypoints  # [[x, y, conf], ...] (pose only)

    def to_dict(self) -> Dict:
        """Convert to dictionary for JSON serialization."""
        d: Dict = {
            'label': self.label,
            'confidence': float(self.confidence),
            'bbox': [float(x) for x in self.bbox]
        }
        if self.mask_png_b64 is not None:
            d['mask'] = self.mask_png_b64
        if self.keypoints is not None:
            d['keypoints'] = self.keypoints
        return d


class ClassificationResult:
    """Single classification result for classify models."""

    def __init__(self, label: str, confidence: float):
        self.label = label
        self.confidence = confidence

    def to_dict(self) -> Dict:
        return {
            'label': self.label,
            'confidence': float(self.confidence),
        }


class InferenceEngine:
    """
    YOLO inference engine wrapper.
    Handles model retrieval and prediction execution.
    Supports detect, segment, semantic_seg, classify, and pose task types.
    """

    def __init__(self):
        self.model_manager = ModelManager()

    def predict(
        self,
        image_source: Union[str, bytes],
        model_id: str,
        confidence_threshold: float = 0.5,
        task_type: str = 'detect',
        iou_threshold: float = 0.5
    ) -> tuple:
        """
        Run inference on an image.

        Args:
            image_source: Image URL, local path, or bytes
            model_id: ID of the model to use
            confidence_threshold: Minimum confidence for detections
            task_type: One of 'detect', 'segment', 'semantic_seg', 'classify', 'pose'
            iou_threshold: IoU threshold for NMS

        Returns:
            For detect/segment/pose: (list of InferenceResult, inference_time_ms)
            For classify: (list of ClassificationResult, inference_time_ms)

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
            iou=iou_threshold,
            verbose=False
        )
        inference_time_ms = (time.time() - start_time) * 1000

        if not results or len(results) == 0:
            return [], inference_time_ms

        result = results[0]

        # Dispatch to task-specific parser
        if task_type == 'classify':
            return self._parse_classify(result, model), inference_time_ms
        elif task_type in ('segment', 'semantic_seg'):
            return self._parse_segment(result, model, image.shape), inference_time_ms
        elif task_type == 'pose':
            return self._parse_pose(result, model), inference_time_ms
        else:
            return self._parse_detect(result, model), inference_time_ms

    def _parse_detect(self, result, model) -> List[InferenceResult]:
        """Parse detection results (bounding boxes)."""
        detections = []
        if hasattr(result, 'boxes') and result.boxes is not None:
            boxes = result.boxes
            for i in range(len(boxes)):
                box = boxes.xyxy[i].cpu().numpy()
                conf = float(boxes.conf[i].cpu().numpy())
                cls_id = int(boxes.cls[i].cpu().numpy())
                label = model.names.get(cls_id, f'class_{cls_id}')
                detections.append(InferenceResult(
                    label=label, confidence=conf, bbox=box.tolist()
                ))
        return detections

    def _parse_segment(self, result, model, image_shape) -> List[InferenceResult]:
        """Parse segmentation results (boxes + per-instance masks as base64 PNG)."""
        import cv2
        detections = []
        if hasattr(result, 'boxes') and result.boxes is not None:
            boxes = result.boxes
            masks = result.masks if hasattr(result, 'masks') else None
            for i in range(len(boxes)):
                box = boxes.xyxy[i].cpu().numpy()
                conf = float(boxes.conf[i].cpu().numpy())
                cls_id = int(boxes.cls[i].cpu().numpy())
                label = model.names.get(cls_id, f'class_{cls_id}')

                mask_b64 = None
                if masks is not None and i < len(masks.data):
                    mask_np = masks.data[i].cpu().numpy()
                    # Resize mask to original image size
                    h, w = image_shape[:2]
                    mask_resized = cv2.resize(mask_np, (w, h), interpolation=cv2.INTER_LINEAR)
                    # Convert to 8-bit single channel
                    mask_uint8 = (mask_resized * 255).astype(np.uint8)
                    _, png_bytes = cv2.imencode('.png', mask_uint8)
                    mask_b64 = base64.b64encode(png_bytes.tobytes()).decode('ascii')

                detections.append(InferenceResult(
                    label=label, confidence=conf, bbox=box.tolist(),
                    mask_png_b64=mask_b64
                ))
        return detections

    def _parse_classify(self, result, model) -> List[ClassificationResult]:
        """Parse classification results (top-N class probabilities)."""
        classifications = []
        if hasattr(result, 'probs') and result.probs is not None:
            probs = result.probs
            top5_indices = probs.top5
            top5_confs = probs.top5conf.cpu().numpy()
            for idx, conf in zip(top5_indices, top5_confs):
                label = model.names.get(idx, f'class_{idx}')
                classifications.append(ClassificationResult(
                    label=label, confidence=float(conf)
                ))
        return classifications

    def _parse_pose(self, result, model) -> List[InferenceResult]:
        """Parse pose estimation results (boxes + keypoints)."""
        detections = []
        if hasattr(result, 'boxes') and result.boxes is not None:
            boxes = result.boxes
            kpts = result.keypoints if hasattr(result, 'keypoints') else None
            for i in range(len(boxes)):
                box = boxes.xyxy[i].cpu().numpy()
                conf = float(boxes.conf[i].cpu().numpy())
                cls_id = int(boxes.cls[i].cpu().numpy())
                label = model.names.get(cls_id, f'class_{cls_id}')

                keypoints_list = None
                if kpts is not None and i < len(kpts.data):
                    kpt_data = kpts.data[i].cpu().numpy()  # shape: (num_kpts, 3)
                    keypoints_list = [[float(x), float(y), float(c)]
                                      for x, y, c in kpt_data]

                detections.append(InferenceResult(
                    label=label, confidence=conf, bbox=box.tolist(),
                    keypoints=keypoints_list
                ))
        return detections
