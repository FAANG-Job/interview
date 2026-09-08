#!/usr/bin/env bash
set -euo pipefail

cd /home/roaggarw/jenkins-jars

podman build -t interview-app:1.0 .

podman rm -f interview-app 2>/dev/null || true

podman run -d \
  --name interview-app \
  -p 8080:8080 \
  interview-app:1.0