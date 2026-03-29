"""Simple tests for inference engine."""
import unittest
import numpy as np
from inference.engine import InferenceEngine
from inference.model_manager import ModelManager


class TestInference(unittest.TestCase):
    """Unit tests for inference functionality."""

    def setUp(self):
        """Set up test fixtures."""
        self.engine = InferenceEngine()
        self.manager = ModelManager()

    def test_model_manager_singleton(self):
        """Test that ModelManager is a singleton."""
        manager2 = ModelManager()
        self.assertIs(self.manager, manager2)

    def test_model_not_loaded(self):
        """Test behavior when model is not loaded."""
        model = self.manager.get_model('nonexistent')
        self.assertIsNone(model)

    def test_predict_without_model(self):
        """Test prediction fails without loaded model."""
        # Create a dummy image
        dummy_image = np.zeros((640, 640, 3), dtype=np.uint8)

        with self.assertRaises(ValueError):
            self.engine.predict(
                image_source=dummy_image.tobytes(),
                model_id='nonexistent'
            )


if __name__ == '__main__':
    unittest.main()
