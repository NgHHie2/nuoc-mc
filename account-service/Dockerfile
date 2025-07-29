FROM amazoncorretto:21-alpine

WORKDIR /app
COPY target/account-service-*.jar app.jar

EXPOSE 8082

ENV JAVA_OPTS="-Xmx384m -Xms128m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]