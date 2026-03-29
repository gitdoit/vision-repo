"""Gunicorn configuration for vision-inference service."""

import os

bind = f"{os.getenv('HOST', '0.0.0.0')}:{os.getenv('PORT', '5000')}"
workers = int(os.getenv('GUNICORN_WORKERS', '1'))
threads = int(os.getenv('GUNICORN_THREADS', '4'))
timeout = int(os.getenv('GUNICORN_TIMEOUT', '120'))

# Use gthread for async I/O (stream capture)
worker_class = 'gthread'

# Logging
accesslog = '-'
errorlog = '-'
loglevel = os.getenv('LOG_LEVEL', 'INFO')

# Process naming
proc_name = 'vision-inference'

# Max requests for worker recycling
max_requests = 1000
max_requests_jitter = 100
