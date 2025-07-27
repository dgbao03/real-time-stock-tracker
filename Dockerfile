# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS deps

WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/
COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -DskipTests

FROM deps AS build

WORKDIR /build

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests

FROM build AS extract

WORKDIR /build

RUN java -Djarmode=layertools -jar target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar extract --destination target/extracted

FROM eclipse-temurin:17-jre-jammy AS final

ARG UID=10001

RUN groupadd --system appuser && useradd -s /bin/false --system -g appuser appuser

USER appuser

WORKDIR /app

COPY --from=extract /build/target/extracted/dependencies/ ./
COPY --from=extract /build/target/extracted/spring-boot-loader/ ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]