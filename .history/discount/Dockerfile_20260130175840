FROM openjdk:22-jdk-slim

WORKDIR /app

# Copy gradle wrapper and properties (these are in the discount folder)
COPY gradlew gradle.properties ./
COPY gradle ./gradle

# Copy build files (these are in the discount folder root and app subfolder)
COPY settings.gradle.kts ./
COPY app ./app

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew build --no-daemon

# Expose port
EXPOSE 8082

# Run the application
CMD ["./gradlew", "run", "--no-daemon"]