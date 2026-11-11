FROM eclipse-temurin:21-jdk AS build
# Without unzip the Maven wrapper downloads the .tar.gz, which fails its check against the .zip checksum.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build --chown=app /build/target/quarkus-app/ ./
USER app
EXPOSE 9999
ENTRYPOINT ["java", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", "-jar", "quarkus-run.jar"]
