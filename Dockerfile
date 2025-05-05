FROM eclipse-temurin:17-jdk

ARG JAR_FILE=couponmoa-coupon/build/libs/couponmoa-store-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod"]