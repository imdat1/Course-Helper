#!/bin/bash
set -e

# Start Celery worker in background
celery -A celery_worker worker --loglevel=info &

# Start Uvicorn in foreground (main process)
exec uvicorn main:app --host 0.0.0.0 --port 8000
