FROM eclipse-temurin:25-jre-alpine

LABEL maintainer="Marco A. Montilla R."
LABEL app="product-catalog"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY app.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
  "-jar", "app.jar"]
