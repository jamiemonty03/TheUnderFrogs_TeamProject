FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY app/target/team-skeleton.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
