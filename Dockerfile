FROM eclipse-temurin:17-jdk
ARG JAR_FILE=build/libs/couponmoa-store-0.0.1-SNAPSHOT.jar

COPY src/main/resources/application-prod.yml /app/config/application-prod.yml
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.config.location=file:/app/config/application-prod.yml", "--spring.profiles.active=prod", "--debug"]






