#!/bin/bash

echo "🚀 Starting FAST development mode..."

# 1. Start shared infrastructure
echo "📡 Starting shared infrastructure..."
cd infrastructure
docker-compose up -d
cd ..

echo "⏳ Waiting for infrastructure to be ready..."
sleep 15

# 2. Start databases for each service
echo "🗄️ Starting individual databases..."

# Account Service DB
docker run -d \
  --name postgres-account-dev \
  -p 5433:5432 \
  -e POSTGRES_DB=accountservicedb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=1111 \
  --network microservice-network \
  postgres:15

# Learn Service DB  
docker run -d \
  --name postgres-learn-dev \
  -p 5434:5432 \
  -e POSTGRES_DB=learnservicedb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=1111 \
  --network microservice-network \
  postgres:15

# Stats Service DB
docker run -d \
  --name postgres-stats-dev \
  -p 5435:5432 \
  -e POSTGRES_DB=statsservicedb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=1111 \
  --network microservice-network \
  postgres:15

echo "✅ Development environment ready!"
echo ""
echo "🎯 Now start your services locally:"
echo "cd account-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "cd learn-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "cd stats-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "cd api-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo ""
echo "🌐 URLs:"
echo "- API Gateway: http://localhost:8080"
echo "- Eureka: http://localhost:8761"
echo "- Swagger: http://localhost:8080/swagger-ui.html"