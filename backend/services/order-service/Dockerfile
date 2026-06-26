FROM eclipse-temurin:24-jre
WORKDIR /app
COPY services/order-service/target/order-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
