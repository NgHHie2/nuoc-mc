FROM openjdk:21-jdk-slim

WORKDIR /app
COPY target/learn-service-*.jar app.jar

EXPOSE 8084

ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]