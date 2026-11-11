#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if ! docker compose version > /dev/null 2>&1; then
    echo "Docker with the Compose plugin is required." >&2
    exit 1
fi

docker compose up --build -d

echo "Waiting for the service to start..."
for _ in $(seq 1 90); do
    if curl -fs http://localhost:9999/q/health/ready > /dev/null; then
        echo "API:      http://localhost:9999/api/v1"
        echo "Kafka UI: http://localhost:8080"
        exit 0
    fi
    sleep 2
done

echo "The service did not become ready in time. Last log lines:" >&2
docker compose logs --tail 50 app >&2
exit 1
