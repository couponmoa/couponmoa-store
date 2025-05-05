FROM eclipse-temurin:17-jdk

ARG JAR_FILE=build/libs/couponmoa-store-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar
COPY src/main/resources/application-prod.yml /app/config/application-prod.yml

ENTRYPOINT ["java", "-jar", "/app.jar","--spring.config.location=file:/app/config/","--spring.profiles.active=prod"]