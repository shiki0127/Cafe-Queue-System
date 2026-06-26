FROM eclipse-temurin:24-jre
WORKDIR /app
COPY services/marketing-coupon-service/target/marketing-coupon-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
