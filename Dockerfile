FROM openjdk:17-jdk-alpine

ARG JAR_FILE=build/libs/*.jar

COPY build/libs/couponmoa-coupon-*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod"]