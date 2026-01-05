FROM maven:4.0.0-rc-5-amazoncorretto-25-debian-trixie AS build

WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY --from=build /app/target/helloworld-0.0.1-SNAPSHOT.jar ./helloworld.jar

EXPOSE 5100

CMD ["java", "-jar", "helloworld.jar"]