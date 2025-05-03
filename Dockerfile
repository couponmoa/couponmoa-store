FROM openjdk:17-jdk-alpine

ARG JAR_FILE=couponmoa-coupon/build/libs/couponmoa-coupon-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod"]