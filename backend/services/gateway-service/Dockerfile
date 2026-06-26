FROM eclipse-temurin:24-jre
WORKDIR /app
COPY services/gateway-service/target/gateway-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
