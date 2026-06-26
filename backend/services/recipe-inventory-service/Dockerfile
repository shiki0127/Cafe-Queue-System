FROM eclipse-temurin:24-jre
WORKDIR /app
COPY services/recipe-inventory-service/target/recipe-inventory-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
