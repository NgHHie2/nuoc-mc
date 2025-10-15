FROM eclipse-temurin:21-jre-alpine AS builder
COPY ./target/learn-service-*.jar ./app.jar
RUN java -Djarmode=layertools -jar ./app.jar extract

FROM eclipse-temurin:21-jre-alpine

# Tải FFmpeg static
RUN wget https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz \
    && tar -xf ffmpeg-release-amd64-static.tar.xz \
    && cp ffmpeg-*-static/ffmpeg /usr/local/bin/ffmpeg \
    && rm -rf ffmpeg-*-static ffmpeg-release-amd64-static.tar.xz

# Copy layer Spring Boot
COPY --from=builder /dependencies/ ./
COPY --from=builder /spring-boot-loader/ ./
COPY --from=builder /snapshot-dependencies/ ./
COPY --from=builder /application/ ./

# Mở cổng
EXPOSE 8084

# Cấu hình JVM
ENV JAVA_OPTS="-Xmx384m -Xms128m"

# FFmpeg config (đường dẫn trong container)
ENV FFMPEG_PATH="/usr/local/bin/ffmpeg"
ENV TEMP_DIR="/tmp/ffmpeg-temp"
ENV TEMP_CLEANUP_INTERVAL=100000
ENV TEMP_CLEANUP_MIN_AGE=95000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
