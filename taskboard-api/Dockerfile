FROM maven:4.0.0-rc-5-amazoncorretto-25-debian-trixie AS build

WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jdk

WORKDIR /app

# Create data directory for H2 persistence
RUN mkdir -p /data

COPY --from=build /app/target/taskboard-api-0.0.1-SNAPSHOT.jar ./taskboard-api.jar

EXPOSE 5100

CMD ["java", "-jar", "taskboard-api.jar"]
