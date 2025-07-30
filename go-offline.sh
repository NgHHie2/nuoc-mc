#!/bin/bash

# Go offline Eureka Server
cd discovery-server
mvn dependency:go-offline
cd ..

# Go offline API Gateway
cd api-gateway
mvn dependency:go-offline
cd ..

# Go offline Account Service
cd account-service
mvn dependency:go-offline
cd ..

# Go offline Learn Service
cd learn-service
mvn dependency:go-offline
cd ..

# Go offline Stats Service
cd stats-service
mvn dependency:go-offline
cd ..

echo "All services go offline successfully!"