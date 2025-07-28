#!/bin/bash

# Run API Gateway container
cd api-gateway
docker compose -f docker-compose.dev.yml up -d
cd ..

# Run Account Service container
cd account-service
docker compose -f docker-compose.dev.yml up -d
cd ..

# Run Learn Service container
cd learn-service
docker compose -f docker-compose.dev.yml up -d
cd ..

# Run Stats Service container
cd stats-service
docker compose -f docker-compose.dev.yml up -d
cd ..

echo "All services run successfully!"