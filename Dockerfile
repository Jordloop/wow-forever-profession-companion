FROM node:24-alpine AS frontend-build

WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --ignore-scripts
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:25-jdk-alpine AS backend-build

RUN apk add --no-cache unzip
WORKDIR /workspace
COPY backend/ backend/
COPY --from=frontend-build /workspace/frontend/dist/frontend/browser/ backend/src/main/resources/static/
RUN ./backend/mvnw -B -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app
RUN addgroup -S companion \
    && adduser -S -u 10001 -G companion companion \
    && mkdir -p /app/logs \
    && chown -R companion:companion /app
COPY --from=backend-build --chown=10001:10001 /workspace/backend/target/*.jar /app/app.jar

USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
