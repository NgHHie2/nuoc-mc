#!/bin/bash

echo "🛑 Stopping development environment..."

# Stop individual databases
docker stop postgres-account-dev postgres-learn-dev postgres-stats-dev 2>/dev/null

# Stop infrastructure
cd infrastructure
docker-compose down
cd ..

echo "✅ Development environment stopped!"