FROM openjdk:17-jdk-alpine

ARG JAR_FILE=build/libs/*.jar

COPY build/libs/couponmoa-backend-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod"]