FROM eclipse-temurin:24-jre
WORKDIR /app
COPY services/vending-device-service/target/vending-device-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
