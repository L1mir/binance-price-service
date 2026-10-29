# Build stage: full JDK and Maven wrapper, discarded after the jar is built
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependencies are downloaded in a separate layer, so they are re-downloaded only when pom.xml changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Tests are not run here: the integration test needs Docker (Testcontainers); run them with ./mvnw test
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package

# Runtime stage: only the JRE and the jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
USER app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
