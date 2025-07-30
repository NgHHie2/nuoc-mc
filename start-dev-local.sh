#!/bin/bash

echo "🚀 Starting FAST development mode..."

# 1. Start shared infrastructure (including all databases)
echo "📡 Starting shared infrastructure and databases..."
cd infrastructure
docker compose up -d
cd ..

echo "⏳ Waiting for infrastructure to be ready..."
sleep 5

echo "✅ Development environment ready!"
echo ""
echo "🎯 Now start your services locally:"
echo "cd account-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "cd learn-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "cd stats-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo ""
echo "🗄️ Database connections:"
echo "- pgAdmin: http://localhost:5050/browser (admin@admin.com / admin123)"
echo "- Account DB: postgres-account-dev:5432 (accountservicedb/postgres/1111)"
echo "- Learn DB: postgres-learn-dev:5432 (learnservicedb/postgres/1111)" 
echo "- Stats DB: postgres-stats-dev:5432 (statsservicedb/postgres/1111)"
echo ""
echo "🌐 URLs:"
echo "- Eureka: http://localhost:8761"
echo "- Swagger: http://localhost:8080/swagger-ui.html"
