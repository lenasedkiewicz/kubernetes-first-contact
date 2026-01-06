FROM maven:4.0.0-rc-5-amazoncorretto-25-debian-trixie AS build

WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY --from=build /app/target/ping-0.0.1-SNAPSHOT.jar ./ping.jar

EXPOSE 8080

CMD ["java", "-jar", "ping.jar"]