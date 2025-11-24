FROM gradle:8.11.1-jdk21-alpine AS builder

WORKDIR /app

# Cache dependencies first
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# Copy source and build the boot jar
COPY . .
RUN ./gradlew clean bootJar --no-daemon && \
    JAR_PATH=$(find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" | head -n 1) && \
    cp "$JAR_PATH" /app/app.jar

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get install -y --no-install-recommends \
    nodejs \
    npm \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/app.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar"]
