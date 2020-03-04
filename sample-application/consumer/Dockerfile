# syntax=docker/dockerfile:experimental

FROM maven:latest as mavenBuild
WORKDIR /app
COPY pom.xml .
# 
RUN --mount=type=cache,target=/root/.m2 \
  mvn dependency:resolve-plugins && \
  mvn dependency:resolve && \
  mvn dependency:go-offline
# Copy all other project files and build project
COPY src ./src
RUN mvn -B package -Dmaven.test.skip

FROM java:8
WORKDIR /app
COPY --from=mavenBuild ./app/target/*.jar ./
ENV JAVA_OPTS ""
CMD [ "bash", "-c", "java ${JAVA_OPTS} -jar *.jar -v"]