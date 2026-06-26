FROM eclipse-temurin:24-jre
WORKDIR /app
COPY services/notification-service/target/notification-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
