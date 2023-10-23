FROM maven:3.9.5-amazoncorretto-17-al2023 AS build
COPY ./pom.xml .
RUN ["/usr/local/bin/mvn-entrypoint.sh", "mvn", "verify", "clean", "--fail-never"]
COPY . .
RUN mvn clean package

FROM amazoncorretto:17-alpine3.18
COPY --from=build /target/maha.jar /target/maha.jar
WORKDIR /target
CMD ["java", "-jar", "maha.jar","--spring.profiles.active=k8s"]