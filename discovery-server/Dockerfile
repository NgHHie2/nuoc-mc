FROM amazoncorretto:21-alpine

WORKDIR /app
COPY target/eureka-server-*.jar app.jar

EXPOSE 8761

ENV JAVA_OPTS="-Xmx128m -Xms64m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]