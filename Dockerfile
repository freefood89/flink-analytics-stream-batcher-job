FROM maven:3-eclipse-temurin-11 as builder
COPY . /usr/src/mymaven
WORKDIR /usr/src/mymaven
RUN --mount=type=cache,target=/root/.m2 mvn -f pom.xml clean package

FROM flink:1.18
COPY --from=builder /usr/src/mymaven/target/*.jar /opt/flink/usrlib/artifacts/