FROM eclipse-temurin:21-jre-alpine AS builder
COPY ./target/account-service-*.jar ./app.jar
RUN java -Djarmode=layertools -jar ./app.jar extract

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /dependencies/ ./
COPY --from=builder /spring-boot-loader/ ./
COPY --from=builder /snapshot-dependencies/ ./
COPY --from=builder /application/ ./

EXPOSE 8082

ENV JAVA_OPTS="-Xmx384m -Xms128m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]