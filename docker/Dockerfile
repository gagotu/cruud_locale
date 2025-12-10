# Start with a base image containing Java runtime
# FROM eclipse-temurin:21-jdk
FROM amazoncorretto:21-alpine

# Information around who maintains the image
LABEL author="exprivia.com"

# Add the application's jar to the image
COPY assets/cruud-0.0.1-SNAPSHOT.jar cruud-0.0.1-SNAPSHOT.jar

EXPOSE 8090

# Command to execute the application
ENTRYPOINT ["java", "-jar", "cruud-0.0.1-SNAPSHOT.jar"]