# Build stage: uses an image with Maven already installed, so the build
# never has to download the Maven distribution zip itself (that download,
# via ./mvnw against repo.maven.apache.org, got 429 rate-limited on Render
# and failed the whole deploy - this avoids hitting that URL at all).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
# repo.maven.apache.org occasionally rate-limits (429) or times out during
# dependency resolution on shared CI/build infrastructure. Retry with a
# backoff instead of failing the whole deploy on one bad request. Ends
# with an explicit exit 1 if every attempt fails, so a real failure still
# fails the Docker build instead of silently succeeding (the loop's last
# command otherwise would have been "sleep", which exits 0).
RUN set -e; \
    for i in 1 2 3 4 5; do \
      mvn -B dependency:go-offline && exit 0; \
      echo "Dependency resolution failed, retrying in 20s ($i/5)..."; \
      sleep 20; \
    done; \
    echo "Dependency resolution failed after 5 attempts"; exit 1
COPY src ./src
# go-offline doesn't resolve every plugin-level artifact clean package
# needs (e.g. maven-compiler-plugin, spring-boot-maven-plugin internals),
# so this step can still hit Maven Central directly - same retry logic
# as above, for the same reason.
RUN set -e; \
    for i in 1 2 3 4 5; do \
      mvn -B clean package -DskipTests && exit 0; \
      echo "Build failed, retrying in 20s ($i/5)..."; \
      sleep 20; \
    done; \
    echo "Build failed after 5 attempts"; exit 1

# Runtime stage: slim JRE only, just the built jar - smaller image, no
# build tools shipped to production.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/employee-management-system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
