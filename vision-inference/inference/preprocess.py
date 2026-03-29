"""Image preprocessing utilities."""
import io
from typing import Union
from urllib.request import urlopen

import cv2
import numpy as np


def load_image(image_source: Union[str, bytes]) -> np.ndarray:
    """
    Load image from URL, local path, or bytes.

    Args:
        image_source: URL string, local file path, or image bytes

    Returns:
        Image as numpy array in BGR format (OpenCV default)
    """
    if isinstance(image_source, bytes):
        # Load from bytes
        nparr = np.frombuffer(image_source, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if image is None:
            raise ValueError("Failed to decode image from bytes")
        return image

    if isinstance(image_source, str):
        if image_source.startswith(('http://', 'https://')):
            # Load from URL
            try:
                with urlopen(image_source, timeout=10) as response:
                    image_bytes = response.read()
                nparr = np.frombuffer(image_bytes, np.uint8)
                image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                if image is None:
                    raise ValueError(f"Failed to decode image from URL: {image_source}")
                return image
            except Exception as e:
                raise ValueError(f"Failed to load image from URL: {e}")

        # Load from local file path
        image = cv2.imread(image_source)
        if image is None:
            raise ValueError(f"Failed to load image from path: {image_source}")
        return image

    raise ValueError(f"Unsupported image source type: {type(image_source)}")


def prepare_for_inference(image: np.ndarray) -> np.ndarray:
    """
    Basic preprocessing for YOLO inference.
    YOLO handles resizing internally, so we just return the image as-is.
    Additional preprocessing can be added here if needed.

    Args:
        image: Input image in BGR format

    Returns:
        Preprocessed image
    """
    # YOLO handles all preprocessing internally
    return image
