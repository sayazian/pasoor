#!/usr/bin/env bash
set -euo pipefail

stop_port() {
  local port="$1"
  local label="$2"

  local pids
  pids=$(lsof -tiTCP:"$port" -sTCP:LISTEN || true)

  if [ -z "$pids" ]; then
    echo "$label: nothing running on port $port"
    return
  fi

  echo "$label: stopping process on port $port: $pids"
  kill $pids
}

stop_port 8080 "Backend"
stop_port 5173 "Frontend"
stop_port 5174 "Frontend fallback"

echo "Done."
