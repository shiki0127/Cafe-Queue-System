FROM eclipse-temurin:24-jre
WORKDIR /app
COPY services/vending-queue-service/target/vending-queue-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
