FROM eclipse-temurin:17-jdk

ARG JAR_FILE=build/libs/couponmoa-store-0.0.1-SNAPSHOT.jar

# JAR 파일 복사
COPY ${JAR_FILE} app.jar

# config 파일을 JAR 내부에서 읽도록 classpath에 포함시킴
COPY src/main/resources/application-prod.yml src/main/resources/application-prod.yml

# ENTRYPOINT에서 spring.config.location 제거
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod", "--debug"]