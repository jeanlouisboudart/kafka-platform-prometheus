FROM maven:latest as mavenBuild
WORKDIR /app
COPY pom.xml .
# To resolve dependencies in a safe way (no re-download when the source code changes)
RUN  mvn clean package -Dmaven.main.skip -Dmaven.test.skip && rm -r target
# Copy all other project files and build project
COPY src ./src
RUN mvn clean package -Dmaven.test.skip

FROM java:8
WORKDIR /app
COPY --from=mavenBuild ./app/target/*.jar ./
ENV JAVA_OPTS ""
CMD [ "bash", "-c", "java ${JAVA_OPTS} -jar *.jar workshop_topic_2"]
