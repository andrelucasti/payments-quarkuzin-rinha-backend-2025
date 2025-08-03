####
# Multi-stage build for Quarkus native
# Stage 1: Build the native binary
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS build
COPY --chown=quarkus:quarkus mvnw /code/mvnw
COPY --chown=quarkus:quarkus .mvn /code/.mvn
COPY --chown=quarkus:quarkus pom.xml /code/
USER quarkus
WORKDIR /code
RUN ./mvnw -B org.apache.maven.plugins:maven-dependency-plugin:3.2.0:go-offline
COPY --chown=quarkus:quarkus src /code/src
RUN ./mvnw package -Dnative -DskipTests

# Stage 2: Use the existing Dockerfile.native structure
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.5
WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work
COPY --from=build --chown=1001:root --chmod=0755 /code/target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]