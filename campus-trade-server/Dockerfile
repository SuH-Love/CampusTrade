FROM openjdk:11-jdk-slim
VOLUME /tmp
COPY target/campus-trade-server-1.0.0.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
