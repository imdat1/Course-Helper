# Stage 1: builder
FROM python:3.10-slim AS builder
WORKDIR /app

# Install uv for faster dependency installation
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

# Copy pyproject.toml and install dependencies
COPY pyproject.toml .
RUN uv pip install --system --no-cache -r pyproject.toml

# Stage 2: final image
FROM python:3.10-slim
WORKDIR /app

# Use HTTPS for apt and update DNS for reliability
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
    libreoffice \
    #TODO: unoconv // Change to use unoserver, new implementation  
    libgl1 \
    libglib2.0-0 \
    poppler-utils \
    tesseract-ocr \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV REDIS_HOST=redis \
    REDIS_PORT=6379 \
    REDIS_DB=0 \
    QDRANT_HOST=qdrant \
    QDRANT_PORT=6333 \
    GOOGLE_GENAI_UPLOAD_TIMEOUT=1000

# Copy installed Python dependencies from builder
COPY --from=builder /usr/local/lib/python3.10/site-packages /usr/local/lib/python3.10/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy application code separately for better caching
COPY ./app ./app
COPY *.py ./
COPY start.sh ./
COPY original_files_for_patching/_api_client.py /usr/local/lib/python3.10/site-packages/google/genai/_api_client.py
COPY original_files_for_patching/files.py /usr/local/lib/python3.10/site-packages/google/genai/files.py

# Ensure permissions for the application
RUN chmod -R 755 /app && chmod +x /app/start.sh

EXPOSE 8000

# Use startup script to run both uvicorn and celery
CMD ["./start.sh"]
