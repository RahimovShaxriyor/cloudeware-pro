FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
ARG SERVICE_MODULE=services/platform-service
COPY pom.xml .
COPY cloudware-common ./cloudware-common
COPY services ./services
RUN mvn -q -pl ${SERVICE_MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG SERVICE_MODULE=services/platform-service
ARG SERVICE_JAR=platform-service-1.0.0.jar
COPY --from=build /app/${SERVICE_MODULE}/target/${SERVICE_JAR} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
