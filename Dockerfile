# Build stage: uses an image with Maven already installed, so the build
# never has to download the Maven distribution zip itself (that download,
# via ./mvnw against repo.maven.apache.org, got 429 rate-limited on Render
# and failed the whole deploy - this avoids hitting that URL at all).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml maven-settings.xml ./
# repo.maven.apache.org has repeatedly 429 rate-limited this build (confirmed
# directly in Render's build logs: "status code: 429, reason phrase: Too
# Many Requests" resolving the parent POM). maven-settings.xml mirrors
# Maven Central through Google's GCS-backed mirror instead, which serves
# the same artifacts without that origin-server rate limit. The retry loop
# stays as defense-in-depth for any other transient failure; it ends with
# an explicit exit 1 if every attempt fails, so a real failure still fails
# the Docker build instead of silently succeeding (the loop's last command
# otherwise would have been "sleep", which exits 0).
RUN set -e; \
    for i in 1 2 3 4 5; do \
      mvn -B -s maven-settings.xml dependency:go-offline && exit 0; \
      echo "Dependency resolution failed, retrying in 20s ($i/5)..."; \
      sleep 20; \
    done; \
    echo "Dependency resolution failed after 5 attempts"; exit 1
COPY src ./src
RUN set -e; \
    for i in 1 2 3 4 5; do \
      mvn -B -s maven-settings.xml clean package -DskipTests && exit 0; \
      echo "Build failed, retrying in 20s ($i/5)..."; \
      sleep 20; \
    done; \
    echo "Build failed after 5 attempts"; exit 1

# Runtime stage: slim JRE only, just the built jar - smaller image, no
# build tools shipped to production.
FROM eclipse-temurin:21-jre-alpine
# Run as an unprivileged user, not root.
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/employee-management-system-0.0.1-SNAPSHOT.jar app.jar
USER app
ENV PORT=8081 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
  CMD wget -qO- "http://localhost:${PORT}/actuator/health" >/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
