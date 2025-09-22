# ============================================================================
# 🐳 DOCKERFILE MULTI-ESTÁGIO - MICROSERVIÇO AUDITORIA & COMPLIANCE
# ============================================================================
#
# Dockerfile otimizado para microserviço reativo com:
# - Multi-stage build para reduzir tamanho da imagem
# - Java 25 com Amazon Corretto (otimizado pela AWS para produção)
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
FROM amazoncorretto:25-alpine3.22 AS builder

# Instalar Maven
ENV MAVEN_VERSION=3.9.11
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

RUN apk add --no-cache wget bash && \
    wget -q https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    tar -xzf apache-maven-$MAVEN_VERSION-bin.tar.gz -C /opt && \
    mv /opt/apache-maven-$MAVEN_VERSION $MAVEN_HOME && \
    rm apache-maven-$MAVEN_VERSION-bin.tar.gz

# Metadados da imagem
LABEL maintainer="Conexão de Sorte <tech@conexaodesorte.com>"
LABEL description="Microserviço de Auditoria e Compliance - Build Stage"
LABEL version="1.0.0"

# Instalar dependências necessárias
RUN apk add --no-cache \
    curl \
    git \
    && rm -rf /var/cache/apk/*

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
FROM amazoncorretto:25-alpine3.22 AS runtime

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
COPY --from=builder /build/target/*.jar app.jar

# Copiar script de entrypoint
COPY docker/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Preparar diretório de logs gravável pelo app
RUN mkdir -p /app/logs

# Health check nativo
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8085/actuator/health || exit 1

# Mudar propriedades para o usuário não-root
RUN chown -R appuser:appgroup /app

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
