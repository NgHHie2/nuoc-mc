#!/bin/bash

echo "🚀 Starting FAST development mode..."

# 1. Start shared infrastructure (including all databases)
echo "📡 Starting shared infrastructure and databases..."
cd infrastructure
docker-compose up -d
cd ..

echo "⏳ Waiting for infrastructure to be ready..."
sleep 20

echo "✅ Development environment ready!"
echo ""
echo "🎯 Now start your services locally:"
echo "cd account-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "cd learn-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "cd stats-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "cd api-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo ""
echo "🗄️ Database connections:"
echo "- Account DB: localhost:5433 (accountservicedb)"
echo "- Learn DB: localhost:5434 (learnservicedb)" 
echo "- Stats DB: localhost:5435 (statsservicedb)"
echo "- PgAdmin: http://localhost:5050 (admin@admin.com / admin123)"
echo ""
echo "🌐 URLs:"
echo "- API Gateway: http://localhost:8080"
echo "- Eureka: http://localhost:8761"
echo "- Swagger: http://localhost:8080/swagger-ui.html"