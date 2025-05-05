FROM eclipse-temurin:17-jdk

ARG JAR_FILE=build/libs/couponmoa-store-0.0.1-SNAPSHOT.jar

# JAR 파일 복사
COPY ${JAR_FILE} app.jar

# application-prod.yml을 외부 config 경로에 복사
COPY src/main/resources/application-prod.yml /app/config/application-prod.yml

# ENTRYPOINT에서 spring.config.location을 명시적으로 외부 경로로 설정
ENTRYPOINT [
  "java", "-jar", "/app.jar",  "--spring.config.location=file:/app/config/", "--spring.profiles.active=prod" ]