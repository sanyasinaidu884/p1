FROM maven:latest AS build
COPY ./ ./
#RUN mkdir root/.m2/  
#COPY settings.xml root/.m2/settings.xml
RUN mvn package -DskipTests=true

FROM openjdk:latest
EXPOSE 8080
#ARG AWS_ACCESS_KEY
#ARG AWS_SECRET_ACCESS_KEY
#COPY aws_config.sh  .
#RUN chmod +x aws_config.sh
#RUN ./aws_config.sh
COPY --from=build /target/spring-boot-docker.jar spring-boot-docker.jar
ENTRYPOINT ["java","-jar","/spring-boot-docker.jar"]
