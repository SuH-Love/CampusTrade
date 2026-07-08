FROM openjdk:11-jdk-slim
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
VOLUME /tmp
COPY target/campus-trade-server-1.0.0.jar app.jar
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
