#!/bin/bash

echo "🛑 Stopping development environment..."

# Stop infrastructure (including all databases)
cd infrastructure
docker compose down
cd ..

echo "✅ Development environment stopped!"