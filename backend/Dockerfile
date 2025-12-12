# Build stage
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy Maven files
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Copy source code
COPY src ./src

# Build the application
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/chat-backend-1.0.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]