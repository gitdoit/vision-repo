"""Flask application for vision-inference service."""
import os
import logging
import signal
import sys
from flask import Flask, request, jsonify
from werkzeug.exceptions import BadRequest

from config import Config
from inference.engine import InferenceEngine
from inference.model_manager import ModelManager
from state_store import StateStore
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
state_store = StateStore(Config.STATE_FILE_PATH)
inference_engine = InferenceEngine()
model_manager = ModelManager()
model_manager.set_state_store(state_store)
stream_manager = StreamTaskManager()

# Restore models from previous session
model_manager.restore_models()

# Node registration (runs in background thread)
_node_registration = None
if Config.ADMIN_URL:
    try:
        from registration import NodeRegistration
        _node_registration = NodeRegistration(
            admin_url=Config.ADMIN_URL,
            node_name=Config.NODE_NAME,
            port=Config.PORT,
            state_store=state_store,
            model_manager=model_manager,
            stream_manager=stream_manager,
            heartbeat_interval=Config.HEARTBEAT_INTERVAL,
            advertise_host=Config.ADVERTISE_HOST,
        )
        _node_registration.register(max_retries=0, retry_delay=5)
        _node_registration.start_heartbeat()
    except Exception as e:
        logger.error('Node registration failed: %s', e)


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
    Supports two modes:
      1. download_url: the model file is downloaded automatically
      2. model_path: use a pre-existing local file path

    Request:
        {
            "model_id": "...",
            "download_url": "http://...",   # preferred
            "model_path": "/models/xxx.pt", # fallback (local path)
            "device": "gpu"                 # optional, defaults to config
        }
    """
    try:
        data = request.get_json()
        if not data:
            raise BadRequest('Request body is required')

        model_id = data.get('model_id')
        download_url = data.get('download_url')
        model_path = data.get('model_path')
        device = data.get('device', Config.DEVICE)

        if not model_id:
            raise BadRequest('model_id is required')
        if not download_url and not model_path:
            raise BadRequest('download_url or model_path is required')

        # Resolve relative paths against base path
        if model_path and not os.path.isabs(model_path):
            model_path = os.path.join(Config.MODEL_BASE_PATH, model_path)

        model_manager.load_model(model_id, model_path=model_path,
                                 device=device, download_url=download_url)
        logger.info(f'Model loaded: {model_id}')

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


@app.route('/models/parse', methods=['POST'])
def parse_model():
    """
    Parse model metadata (class names, task type, input size).
    Accepts either multipart file upload or JSON with model_path.

    Response:
        {
            "success": true,
            "class_names": ["person", "car"],
            "num_classes": 2,
            "task_type": "detect",
            "input_size": 640
        }
    """
    from ultralytics import YOLO
    import tempfile

    tmp_path = None
    try:
        if request.content_type and 'multipart' in request.content_type:
            if 'file' not in request.files:
                raise BadRequest('file is required')
            file = request.files['file']
            fd, tmp_path = tempfile.mkstemp(suffix='.pt')
            os.close(fd)
            file.save(tmp_path)
            model_path = tmp_path
        else:
            data = request.get_json()
            if not data or not data.get('model_path'):
                raise BadRequest('model_path is required')
            model_path = data['model_path']

        model = YOLO(model_path)

        # Extract metadata
        names = model.names  # dict: {0: 'person', 1: 'car', ...}
        class_names = list(names.values()) if names else []
        num_classes = len(class_names)

        task_type = getattr(model, 'task', 'detect') or 'detect'

        input_size = 640
        try:
            if hasattr(model, 'overrides') and model.overrides.get('imgsz'):
                imgsz = model.overrides['imgsz']
                input_size = imgsz if isinstance(imgsz, int) else imgsz[0]
        except Exception:
            pass

        return jsonify({
            'success': True,
            'class_names': class_names,
            'num_classes': num_classes,
            'task_type': task_type,
            'input_size': input_size,
        })

    except BadRequest as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        logger.error('Model parse error: %s', e)
        return jsonify({'error': str(e)}), 500
    finally:
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.unlink(tmp_path)
            except OSError:
                pass


@app.route('/system/info', methods=['GET'])
def system_info():
    """Return system hardware and runtime information."""
    import platform
    info = {
        'hostname': platform.node(),
        'os': platform.system(),
        'cpu': platform.processor() or platform.machine(),
        'device': Config.DEVICE,
    }

    try:
        import psutil
        mem = psutil.virtual_memory()
        info['memory_total'] = mem.total
        info['memory_used'] = mem.used
        info['cpu_percent'] = psutil.cpu_percent(interval=0.1)
    except ImportError:
        pass

    try:
        import torch
        info['cuda_available'] = torch.cuda.is_available()
        if torch.cuda.is_available():
            info['gpu_name'] = torch.cuda.get_device_name(0)
            info['gpu_count'] = torch.cuda.device_count()
    except ImportError:
        info['cuda_available'] = False

    info['loaded_models'] = len(model_manager.list_models())
    info['active_tasks'] = len(stream_manager.list_tasks())

    if _node_registration:
        info['node_id'] = _node_registration.node_id

    return jsonify(info)


@app.route('/admin/reload', methods=['POST'])
def admin_reload():
    """
    Reload the inference service.
    Under Gunicorn: sends SIGHUP to master process to gracefully restart workers.
    Dev mode (python app.py): schedules process restart.
    """
    is_gunicorn = 'gunicorn' in os.environ.get('SERVER_SOFTWARE', '')

    if is_gunicorn:
        master_pid = os.getppid()
        logger.info('Reload requested: sending SIGHUP to gunicorn master (PID %d)', master_pid)
        os.kill(master_pid, signal.SIGHUP)
        return jsonify({'success': True, 'mode': 'gunicorn', 'message': 'SIGHUP sent to master'})
    else:
        logger.info('Reload requested: scheduling process restart')

        def _restart():
            import time
            time.sleep(1)
            os.execv(sys.executable, [sys.executable] + sys.argv)

        import threading
        threading.Thread(target=_restart, daemon=True).start()
        return jsonify({'success': True, 'mode': 'dev', 'message': 'Restart scheduled'})


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
