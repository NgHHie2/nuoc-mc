#!/bin/bash

echo "Building all microservices..."

# Build Eureka Server
echo "Building Eureka Server..."
cd discovery-server
mvn clean package -DskipTests
docker build -t eureka-server:latest .
cd ..

# Build API Gateway
echo "Building API Gateway..."
cd api-gateway
mvn clean package -DskipTests
docker build -t api-gateway:latest .
cd ..

# Build Account Service
echo "Building Account Service..."
cd account-service
mvn clean package -DskipTests
docker build -t account-service:latest .
cd ..

# Build Learn Service
echo "Building Learn Service..."
cd learn-service
mvn clean package -DskipTests
docker build -t learn-service:latest .
cd ..

# Build Stats Service
echo "Building Stats Service..."
cd stats-service
mvn clean package -DskipTests
docker build -t stats-service:latest .
cd ..

echo "All services built successfully!"