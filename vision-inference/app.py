"""Flask application for vision-inference service."""
import os
import logging
from flask import Flask, request, jsonify
from werkzeug.exceptions import BadRequest

from config import Config
from inference.engine import InferenceEngine
from inference.model_manager import ModelManager
from stream.stream_task import StreamTaskManager

# Configure logging
logging.basicConfig(
    level=Config.LOG_LEVEL,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)

# Validate configuration
Config.validate()

# Initialize singletons
inference_engine = InferenceEngine()
model_manager = ModelManager()
stream_manager = StreamTaskManager()


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({
        'status': 'healthy',
        'device': Config.DEVICE,
        'loaded_models': len(model_manager.list_models())
    })


@app.route('/predict', methods=['POST'])
def predict():
    """
    Single image inference endpoint.

    Request:
        {
            "image_url": "...",       # URL or local path
            "model_id": "...",
            "confidence_threshold": 0.5,  # optional
            "task_type": "detect"         # optional: detect/segment/classify/pose
        }

    Response (detect/segment/pose):
        {
            "task_type": "detect",
            "objects": [
                {"label": "...", "confidence": 0.95, "bbox": [x1, y1, x2, y2], "mask": "...", "keypoints": [...]}
            ],
            "inference_time_ms": 45
        }

    Response (classify):
        {
            "task_type": "classify",
            "objects": [],
            "classifications": [
                {"label": "cat", "confidence": 0.95}
            ],
            "inference_time_ms": 12
        }
    """
    try:
        data = request.get_json()
        if not data:
            raise BadRequest('Request body is required')

        image_url = data.get('image_url')
        model_id = data.get('model_id')
        confidence_threshold = data.get('confidence_threshold', Config.DEFAULT_CONFIDENCE_THRESHOLD)
        task_type = data.get('task_type', 'detect')
        iou_threshold = data.get('iou_threshold', 0.5)

        if not image_url:
            raise BadRequest('image_url is required')
        if not model_id:
            raise BadRequest('model_id is required')

        # Run inference
        results, inference_time = inference_engine.predict(
            image_source=image_url,
            model_id=model_id,
            confidence_threshold=confidence_threshold,
            task_type=task_type,
            iou_threshold=iou_threshold
        )

        response = {
            'task_type': task_type,
            'inference_time_ms': round(inference_time, 2)
        }

        if task_type == 'classify':
            response['objects'] = []
            response['classifications'] = [r.to_dict() for r in results]
        else:
            response['objects'] = [d.to_dict() for d in results]

        return jsonify(response)

    except BadRequest as e:
        return jsonify({'error': str(e)}), 400
    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        logger.error(f'Prediction error: {e}')
        return jsonify({'error': 'Internal server error'}), 500


@app.route('/models/load', methods=['POST'])
def load_model():
    """
    Load a model into memory.

    Request:
        {
            "model_id": "...",
            "model_path": "/models/xxx.pt",
            "device": "gpu"  # optional, defaults to config
        }
    """
    try:
        data = request.get_json()
        if not data:
            raise BadRequest('Request body is required')

        model_id = data.get('model_id')
        model_path = data.get('model_path')
        device = data.get('device', Config.DEVICE)

        if not model_id:
            raise BadRequest('model_id is required')
        if not model_path:
            raise BadRequest('model_path is required')

        # Resolve relative paths against base path
        if not os.path.isabs(model_path):
            model_path = os.path.join(Config.MODEL_BASE_PATH, model_path)

        model_manager.load_model(model_id, model_path, device)
        logger.info(f'Model loaded: {model_id} from {model_path}')

        return jsonify({'success': True, 'model_id': model_id})

    except BadRequest as e:
        return jsonify({'error': str(e)}), 400
    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        logger.error(f'Model load error: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/models/unload', methods=['POST'])
def unload_model():
    """
    Unload a model from memory.

    Request:
        {"model_id": "..."}
    """
    try:
        data = request.get_json()
        if not data:
            raise BadRequest('Request body is required')

        model_id = data.get('model_id')
        if not model_id:
            raise BadRequest('model_id is required')

        if model_manager.unload_model(model_id):
            logger.info(f'Model unloaded: {model_id}')
            return jsonify({'success': True, 'model_id': model_id})
        else:
            return jsonify({'error': f'Model {model_id} not found'}), 404

    except BadRequest as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        logger.error(f'Model unload error: {e}')
        return jsonify({'error': 'Internal server error'}), 500


@app.route('/models/status', methods=['GET'])
def model_status():
    """
    Get list of loaded models.

    Response:
        {
            "models": {
                "model_id": {
                    "model_id": "...",
                    "model_path": "...",
                    "device": "...",
                    "loaded_at": 1234567890.0,
                    "status": "loaded"
                }
            }
        }
    """
    models = model_manager.list_models()
    return jsonify({
        'models': {
            k: {
                'model_id': v.model_id,
                'model_path': v.model_path,
                'device': v.device,
                'loaded_at': v.loaded_at,
                'status': v.status
            }
            for k, v in models.items()
        }
    })


@app.route('/device/info', methods=['GET'])
def device_info():
    """
    Return available compute devices.

    Response:
        {
            "devices": ["cpu", "cuda"],
            "gpu_name": "NVIDIA GeForce RTX 3090",  # if GPU present
            "cuda_available": true
        }
    """
    import torch
    cuda_available = torch.cuda.is_available()
    devices = ['cpu']
    gpu_name = None
    if cuda_available:
        devices.append('cuda')
        gpu_name = torch.cuda.get_device_name(0)
    return jsonify({
        'devices': devices,
        'cuda_available': cuda_available,
        'gpu_name': gpu_name,
    })


@app.route('/stream/start', methods=['POST'])
def stream_start():
    """
    Start a stream inference task.

    Request:
        {
            "task_id": "...",
            "stream_url": "rtsp://...",
            "model_id": "...",
            "fps": 5,
            "callback_url": "http://..."
        }
    """
    try:
        data = request.get_json()
        if not data:
            raise BadRequest('Request body is required')

        task_id = data.get('task_id')
        stream_url = data.get('stream_url')
        model_id = data.get('model_id')
        fps = data.get('fps', Config.DEFAULT_STREAM_FPS)
        callback_url = data.get('callback_url')

        if not all([task_id, stream_url, model_id, callback_url]):
            raise BadRequest('task_id, stream_url, model_id, and callback_url are required')

        # Check if model is loaded
        if not model_manager.is_loaded(model_id):
            return jsonify({'error': f'Model {model_id} not loaded'}), 400

        # Check task limit
        active_tasks = len(stream_manager.list_tasks())
        if active_tasks >= Config.MAX_STREAM_TASKS:
            return jsonify({'error': f'Maximum stream tasks reached ({Config.MAX_STREAM_TASKS})'}), 400

        if stream_manager.start_task(task_id, stream_url, model_id, fps, callback_url):
            logger.info(f'Stream started: {task_id}')
            return jsonify({'success': True, 'task_id': task_id})
        else:
            return jsonify({'error': f'Task {task_id} already exists'}), 400

    except BadRequest as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        logger.error(f'Stream start error: {e}')
        return jsonify({'error': 'Internal server error'}), 500


@app.route('/stream/stop', methods=['POST'])
def stream_stop():
    """
    Stop a stream inference task.

    Request:
        {"task_id": "..."}
    """
    try:
        data = request.get_json()
        if not data:
            raise BadRequest('Request body is required')

        task_id = data.get('task_id')
        if not task_id:
            raise BadRequest('task_id is required')

        if stream_manager.stop_task(task_id):
            logger.info(f'Stream stopped: {task_id}')
            return jsonify({'success': True, 'task_id': task_id})
        else:
            return jsonify({'error': f'Task {task_id} not found'}), 404

    except BadRequest as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        logger.error(f'Stream stop error: {e}')
        return jsonify({'error': 'Internal server error'}), 500


@app.route('/stream/tasks', methods=['GET'])
def stream_tasks():
    """
    List active stream tasks.

    Response:
        {
            "tasks": {
                "task_id": {
                    "stream_url": "...",
                    "model_id": "...",
                    "fps": 5,
                    "callback_url": "...",
                    "running": true
                }
            }
        }
    """
    return jsonify({'tasks': stream_manager.list_tasks()})


@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors."""
    return jsonify({'error': 'Not found'}), 404


@app.errorhandler(405)
def method_not_allowed(error):
    """Handle 405 errors."""
    return jsonify({'error': 'Method not allowed'}), 405


if __name__ == '__main__':
    # Development server only
    app.run(host=Config.HOST, port=Config.PORT, debug=False)
