# Build the application JAR in the authorized release pipeline; this image only packs it.
FROM eclipse-temurin:21-jdk-jammy AS runtime-prep
ARG JAR_FILE=target/portal-*.jar
COPY ${JAR_FILE} /tmp/portal.jar
COPY docker/HealthCheck.java /tmp/HealthCheck.java
RUN javac /tmp/HealthCheck.java -d /opt/portal-health

FROM eclipse-temurin:21-jre-jammy
LABEL org.opencontainers.image.title="S-portal" \
      org.opencontainers.image.description="Simbio server-rendered modular monolith"
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75" \
    SPRING_PROFILES_ACTIVE="prod"
RUN groupadd --system --gid 10001 portal \
    && useradd --system --uid 10001 --gid portal --home-dir /opt/portal --shell /usr/sbin/nologin portal \
    && mkdir -p /opt/portal /var/lib/portal/files /var/lib/portal/errors \
    && chown -R portal:portal /opt/portal /var/lib/portal
COPY --from=runtime-prep --chown=portal:portal /tmp/portal.jar /opt/portal/portal.jar
COPY --from=runtime-prep --chown=portal:portal /opt/portal-health /opt/portal/health
USER 10001:10001
WORKDIR /opt/portal
EXPOSE 8090
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
  CMD ["java", "-cp", "/opt/portal/health", "HealthCheck", "http://127.0.0.1:8091/actuator/health/readiness"]
ENTRYPOINT ["java", "-jar", "/opt/portal/portal.jar"]
