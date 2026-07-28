# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:26.0.1_8-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar

FROM build AS test

RUN ./gradlew --no-daemon testClasses

CMD ["./gradlew", "--no-daemon", "test"]

FROM eclipse-temurin:26.0.1_8-jre AS runtime

RUN apt-get update && \
    apt-get install --yes --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --system app && \
    useradd --system --gid app --home /app app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
    CMD ["curl", "--fail", "--silent", "http://localhost:8080/actuator/health/readiness"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
