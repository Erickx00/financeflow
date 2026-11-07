# syntax=docker/dockerfile:1
#
# Build da aplicacao. A imagem de build pode ser sobrescrita caso a tag
# escolhida nao exista no seu registry:
#   docker build --build-arg BUILD_IMAGE=maven:3.9-eclipse-temurin-25 .

ARG BUILD_IMAGE=maven:3.9-eclipse-temurin-25
FROM ${BUILD_IMAGE} AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre AS runtime
RUN groupadd --system financeflow && useradd --system --gid financeflow financeflow
WORKDIR /app
COPY --from=build /workspace/target/financeflow-*.jar app.jar
USER financeflow
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
