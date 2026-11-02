# Build: Maven + JDK 17 compilan el jar (los tests corren en CI, no en la imagen)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Runtime: JRE 17 + el jar de Spring Boot
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Railway/Render inyectan PORT; Spring Boot lo lee al mapearlo a server.port
CMD ["sh", "-c", "java -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar app.jar"]
