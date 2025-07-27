FROM openjdk:21-jdk-slim

WORKDIR /app
COPY target/eureka-server-*.jar app.jar

EXPOSE 8761

ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]