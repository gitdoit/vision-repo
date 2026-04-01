"""Node registration and heartbeat module."""
import logging
import platform
import socket
import threading
import time

import requests

logger = logging.getLogger(__name__)


def _get_local_ip() -> str:
    """Detect local IP address."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(1)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return '127.0.0.1'


def _get_system_info() -> dict:
    """Gather system hardware information."""
    import psutil

    info = {
        'cpu_info': platform.processor() or platform.machine(),
        'memory_total': psutil.virtual_memory().total,
        'device_type': 'cpu',
        'gpu_name': None,
        'gpu_count': 0,
    }

    try:
        import torch
        if torch.cuda.is_available():
            info['device_type'] = 'cuda'
            info['gpu_name'] = torch.cuda.get_device_name(0)
            info['gpu_count'] = torch.cuda.device_count()
    except ImportError:
        pass

    return info


def _get_system_load() -> dict:
    """Get current system load metrics."""
    import psutil

    load = {
        'cpuPercent': psutil.cpu_percent(interval=0.1),
        'memoryPercent': psutil.virtual_memory().percent,
    }

    try:
        import torch
        if torch.cuda.is_available():
            # Basic GPU utilization via nvidia-smi would require pynvml
            # For simplicity, report 0 if not available
            load['gpuPercent'] = 0.0
    except ImportError:
        pass

    return load


# Bypass system proxy for internal service communication
_NO_PROXY_SESSION = requests.Session()
_NO_PROXY_SESSION.trust_env = False


class NodeRegistration:
    """Handles registration with the admin service and periodic heartbeat."""

    def __init__(self, admin_url: str, node_name: str, port: int,
                 state_store, model_manager, stream_manager,
                 heartbeat_interval: int = 15, advertise_host: str = ''):
        self._admin_url = admin_url.rstrip('/')
        self._node_name = node_name or socket.gethostname()
        self._port = port
        self._state_store = state_store
        self._model_manager = model_manager
        self._stream_manager = stream_manager
        self._heartbeat_interval = heartbeat_interval
        self._host = advertise_host if advertise_host else _get_local_ip()
        self._node_id = state_store.get_node_id()
        self._heartbeat_thread = None
        self._running = False

    @property
    def node_id(self) -> str | None:
        return self._node_id

    def register(self, max_retries: int = 0, retry_delay: int = 5):
        """
        Register this node with the admin service.

        Args:
            max_retries: 0 = infinite retries
            retry_delay: seconds between retries
        """
        sys_info = _get_system_info()

        payload = {
            'nodeId': self._node_id,
            'nodeName': self._node_name,
            'host': self._host,
            'port': self._port,
            'deviceType': sys_info['device_type'],
            'gpuName': sys_info['gpu_name'],
            'gpuCount': sys_info['gpu_count'],
            'cpuInfo': sys_info['cpu_info'],
            'memoryTotal': sys_info['memory_total'],
        }

        attempt = 0
        while True:
            attempt += 1
            try:
                url = f'{self._admin_url}/api/v1/nodes/register'
                resp = _NO_PROXY_SESSION.post(url, json=payload, timeout=10)
                resp.raise_for_status()

                data = resp.json().get('data', {})
                self._node_id = data.get('id')
                self._state_store.save_node_id(self._node_id)
                logger.info('Node registered: id=%s, name=%s', self._node_id, self._node_name)
                return True

            except Exception as e:
                logger.warning('Registration attempt %d failed: %s', attempt, e)
                if max_retries and attempt >= max_retries:
                    logger.error('Registration failed after %d attempts', max_retries)
                    return False
                time.sleep(retry_delay)

    def start_heartbeat(self):
        """Start the heartbeat daemon thread."""
        if self._heartbeat_thread and self._heartbeat_thread.is_alive():
            return

        self._running = True
        self._heartbeat_thread = threading.Thread(
            target=self._heartbeat_loop, daemon=True, name='heartbeat'
        )
        self._heartbeat_thread.start()
        logger.info('Heartbeat started: interval=%ds', self._heartbeat_interval)

    def _heartbeat_loop(self):
        while self._running:
            try:
                self._send_heartbeat()
            except Exception as e:
                logger.warning('Heartbeat failed: %s', e)
            time.sleep(self._heartbeat_interval)

    def _send_heartbeat(self):
        if not self._node_id:
            return

        # Gather runtime data
        loaded_models = []
        for model_id, info in self._model_manager.list_models().items():
            loaded_models.append({
                'modelId': info.model_id,
                'modelPath': info.model_path,
                'device': info.device,
                'loadedAt': info.loaded_at,
            })

        active_tasks = []
        for task_id, task_info in self._stream_manager.list_tasks().items():
            active_tasks.append({
                'taskId': task_id,
                'streamUrl': task_info.get('stream_url', ''),
                'modelId': task_info.get('model_id', ''),
                'fps': task_info.get('fps', 0),
                'running': task_info.get('running', False),
            })

        payload = {
            'nodeId': self._node_id,
            'loadedModels': loaded_models,
            'activeTasks': active_tasks,
            'systemLoad': _get_system_load(),
        }

        url = f'{self._admin_url}/api/v1/nodes/heartbeat'
        resp = _NO_PROXY_SESSION.post(url, json=payload, timeout=10)
        resp.raise_for_status()

    def shutdown(self):
        """Stop heartbeat."""
        self._running = False
