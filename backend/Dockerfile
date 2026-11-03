FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /build
COPY backend/mvnw ./mvnw
COPY backend/.mvn ./.mvn
COPY backend/pom.xml ./pom.xml
COPY backend/src/main ./src/main
RUN chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
RUN groupadd --gid 10001 intiwasi && useradd --uid 10001 --gid 10001 --no-create-home intiwasi
COPY --from=build --chown=10001:10001 /build/target/backend-0.0.1-SNAPSHOT.jar ./backend.jar
COPY --chown=10001:10001 deploy/application-production.properties.example ./application.properties
COPY --chmod=755 backend/docker/entrypoint.sh ./entrypoint.sh
ENV SERVER_ADDRESS=0.0.0.0 PORT=8080 JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55.0 -Djava.awt.headless=true"
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
