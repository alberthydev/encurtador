# syntax=docker/dockerfile:1

# ---------- Estágio 1: BUILD (JDK + Maven) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache de dependências: só invalida se o pom.xml mudar
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam na pipeline de CI; aqui só empacotamos o .jar
RUN mvn -B -q package -DskipTests

# ---------- Estágio 2: RUNTIME (apenas JRE enxuto) ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuário não-root + diretório de dados do H2
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /data && chown app:app /data

WORKDIR /app
COPY --from=build /workspace/target/url-shortener.jar app.jar

USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
