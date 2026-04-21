FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .

RUN cd "Talksy Backend" && mvn clean package -DskipTests && cp target/*.jar /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
