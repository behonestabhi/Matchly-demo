# Matchly AI service image.
FROM python:3.11-slim

# Avoid .pyc files and buffer-flush issues in containers.
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# Install dependencies first to leverage Docker layer caching.
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application source.
COPY app ./app
COPY pipelines ./pipelines
COPY ml ./ml

EXPOSE 8000

# The AI service is HTTP-only (no Kafka) and listens on port 8000.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
