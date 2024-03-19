#FROM maven:latest AS build
#COPY ./ ./

#RUN mvn package -DskipTests=true

#FROM openjdk:latest
#EXPOSE 8080

#COPY --from=build /target/spring-boot-docker.jar spring-boot-docker.jar
#ENTRYPOINT ["java","-jar","/spring-boot-docker.jar"]


# Use the official Maven image as a build stage
FROM maven:3.8.4-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the project files into the container
COPY . .

# Package the application using Maven
RUN mvn package -DskipTests=true

# Create a new stage for the application runtime
FROM openjdk:17-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file from the build stage to the runtime stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Define the command to run the application when the container starts
CMD ["java", "-jar", "app.jar"]

