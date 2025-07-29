FROM amazoncorretto:21-alpine 

WORKDIR /app
COPY target/stats-service-*.jar app.jar

EXPOSE 8081

ENV JAVA_OPTS="-Xmx384m -Xms128m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]