FROM maven:3.6.3-jdk-11 as mavenBuild
WORKDIR /app
COPY pom.xml .
# To resolve dependencies in a safe way (no re-download when the source code changes)
RUN mvn dependency:go-offline -B
# Copy all other project files and build project
COPY src src
RUN mvn clean install -Dmaven.test.skip -B

FROM adoptopenjdk/openjdk11:alpine
RUN apk add bash
WORKDIR /app
COPY --from=mavenBuild ./app/target/*.jar ./
ENV JAVA_OPTS ""
CMD [ "bash", "-c", "java ${JAVA_OPTS} -jar *.jar"]
