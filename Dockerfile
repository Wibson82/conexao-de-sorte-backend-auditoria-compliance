# ============================================================================
# 🐳 DOCKERFILE MULTI-ESTÁGIO - MICROSERVIÇO AUDITORIA & COMPLIANCE
# ============================================================================
#
# Dockerfile otimizado para microserviço reativo com:
# - Multi-stage build para reduzir tamanho da imagem
# - Java 24 com JVM otimizada para containers
# - Usuário não-root para segurança
# - Health check nativo
# - Otimizações de performance
# - Sem hardcoded secrets (usa external secrets)
#
# Build: docker build -t conexaodesorte/auditoria:latest .
# Run: docker run -p 8085:8085 conexaodesorte/auditoria:latest
#
# @author Sistema de Migração R2DBC
# @version 1.0
# @since 2024
# ============================================================================

# === ESTÁGIO 1: BUILD ===
FROM maven:3.9.11-eclipse-temurin-24-alpine AS builder

# Metadados da imagem
LABEL maintainer="Conexão de Sorte <tech@conexaodesorte.com>"
LABEL description="Microserviço de Auditoria e Compliance - Build Stage"
LABEL version="1.0.0"

# Variáveis de build
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

# Definir diretório de trabalho
WORKDIR /build

# Copiar arquivos de configuração Maven (cache layer)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Download de dependências (layer cacheável) com timeout estendido
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B \
    -Dmaven.wagon.http.connectionTimeout=300000 \
    -Dmaven.wagon.http.readTimeout=300000 \
    -Dmaven.wagon.rto=300000

# Copiar código fonte
COPY src/ src/

# Build da aplicação com otimizações e timeout estendido
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B \
    -Dspring-boot.build-image.pullPolicy=IF_NOT_PRESENT \
    -Dmaven.compiler.debug=false \
    -Dmaven.compiler.optimize=true \
    -Dmaven.wagon.http.connectionTimeout=300000 \
    -Dmaven.wagon.http.readTimeout=300000 \
    -Dmaven.wagon.rto=300000

# === ESTÁGIO 2: RUNTIME ===
FROM eclipse-temurin:24-jre-alpine AS runtime

# Instalar dependências do sistema
RUN apk add --no-cache \
    tzdata \
    curl \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Configurar timezone
ENV TZ=America/Sao_Paulo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Criar usuário não-root para segurança
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Definir diretório da aplicação
WORKDIR /app

# Copiar JAR da aplicação do estágio de build
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Preparar diretório de logs gravável pelo app
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app/logs

# Expor porta da aplicação
EXPOSE 8085

# Health check nativo
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8085/actuator/health || exit 1

# Script de entrada com pré-checagem de conexão ao banco (no-op se não houver secrets)
RUN printf '%s\n' '#!/bin/sh' \
    'set -eu' \
    'log() { printf "%s %s\\n" "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$*"; }' \
    'SECRETS_DIR=${SECRETS_DIR:-/run/secrets}' \
    'R2DBC_FILE="$SECRETS_DIR/spring.r2dbc.url"' \
    'JDBC_FILE="$SECRETS_DIR/spring.flyway.url"' \
    'has_nc() { command -v nc >/dev/null 2>&1; }' \
    'can_connect() { host="$1"; port="$2"; if has_nc; then nc -z -w 2 "$host" "$port" >/dev/null 2>&1; else (echo > /dev/tcp/"$host"/"$port") >/dev/null 2>&1 || return 1; fi; }' \
    'rewrite_urls() { new_hostport="$1"; if [ -f "$R2DBC_FILE" ]; then r2dbc=$(cat "$R2DBC_FILE"); proto="r2dbc:mysql://"; rest="${r2dbc#${proto}}"; rest_no_host="${rest#*/}"; echo "${proto}${new_hostport}/${rest_no_host}" > "$R2DBC_FILE"; fi; if [ -f "$JDBC_FILE" ]; then jdbc=$(cat "$JDBC_FILE"); proto="jdbc:mysql://"; rest="${jdbc#${proto}}"; rest_no_host="${rest#*/}"; echo "${proto}${new_hostport}/${rest_no_host}" > "$JDBC_FILE"; fi; }' \
    'preflight_db() {' \
    '  [ -f "$R2DBC_FILE" ] || return 0' \
    '  url=$(cat "$R2DBC_FILE"); base="${url#r2dbc:mysql://}"; hostport="${base%%/*}"; host="${hostport%%:*}"; port="${hostport#*:}"; [ "$port" = "$host" ] && port=3306' \
    '  if can_connect "$host" "$port"; then return 0; fi' \
    '  for alt in "conexao-mysql" "host.docker.internal"; do if can_connect "$alt" "$port"; then rewrite_urls "$alt:$port"; return 0; fi; done' \
    '  gw=$(ip route 2>/dev/null | awk "/default/ {print $3; exit}"); if [ -n "${gw:-}" ] && can_connect "$gw" "$port"; then rewrite_urls "$gw:$port"; return 0; fi' \
    '  if can_connect 127.0.0.1 "$port" || can_connect localhost "$port"; then rewrite_urls "127.0.0.1:$port"; fi' \
    '}' \
    'preflight_db || true' \
    'exec dumb-init -- java -jar /app/app.jar' \
    > /app/docker-entrypoint.sh && chmod +x /app/docker-entrypoint.sh && chown appuser:appgroup /app/docker-entrypoint.sh

# Mudar para usuário não-root
USER appuser:appgroup

# Labels para metadata
LABEL org.opencontainers.image.title="Conexão de Sorte - Auditoria"
LABEL org.opencontainers.image.description="Microserviço de Auditoria e Compliance"
LABEL org.opencontainers.image.version=${VERSION}
LABEL org.opencontainers.image.created=${BUILD_DATE}
LABEL org.opencontainers.image.revision=${VCS_REF}
LABEL org.opencontainers.image.vendor="Conexão de Sorte"
LABEL org.opencontainers.image.licenses="MIT"
LABEL org.opencontainers.image.url="https://conexaodesorte.com"
LABEL org.opencontainers.image.source="https://github.com/conexaodesorte/auditoria-compliance"

# Comando de inicialização com pré-checagem de DB (no-op se sem secrets)
ENTRYPOINT ["/app/docker-entrypoint.sh"]

# === ESTÁGIO 3: DEBUG (Opcional) ===
FROM runtime AS debug

# Configurar debug remoto
ENV JAVA_OPTS="$JAVA_OPTS \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
    -Dspring.profiles.active=dev \
    -Dlogging.level.br.tec.facilitaservicos=DEBUG"

# Expor porta de debug
EXPOSE 5005

# Comando para debug
CMD ["sh", "-c", "echo 'Starting in DEBUG mode on port 5005' && java $JAVA_OPTS -jar app.jar"]

# === ESTÁGIO FINAL: RELEASE (Padrão) ===
# Garante que o build padrão (sem --target) produza a imagem de runtime
FROM runtime AS release
ENTRYPOINT ["dumb-init", "--", "java"]
CMD ["-jar", "app.jar"]
