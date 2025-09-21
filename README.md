# 📊 Microserviço de Auditoria & Compliance

## 🎯 Visão Geral

Microserviço responsável por auditoria, compliance e integridade de dados no ecossistema Conexão de Sorte. Implementa controles LGPD, SOX e trilhas de auditoria imutáveis com validação criptográfica.

## 🔐 Configuração de Segredos

### **GitHub Repository Variables (vars)**
```env
AZURE_CLIENT_ID=<client-id>
AZURE_TENANT_ID=<tenant-id>
AZURE_SUBSCRIPTION_ID=<subscription-id>
AZURE_KEYVAULT_NAME=<vault-name>
AZURE_KEYVAULT_ENDPOINT=https://<vault-name>.vault.azure.net/
```

### **Azure Key Vault Secrets (12 necessários)**

#### **Banco de Dados (3)**
- `conexao-de-sorte-database-r2dbc-url`
- `conexao-de-sorte-database-username`
- `conexao-de-sorte-database-password`

#### **Redis (4)**
- `conexao-de-sorte-redis-host`
- `conexao-de-sorte-redis-port`
- `conexao-de-sorte-redis-password`
- `conexao-de-sorte-redis-database`

#### **JWT (5)**
- `conexao-de-sorte-jwt-issuer`
- `conexao-de-sorte-jwt-jwks-uri`
- `conexao-de-sorte-jwt-secret`
- `conexao-de-sorte-jwt-signing-key`
- `conexao-de-sorte-jwt-verification-key`

## 🏃‍♂️ Runners Self-Hosted

### **Labels Obrigatórios**
```yaml
runs-on: [self-hosted, Linux, X64, srv649924, conexao, conexao-de-sorte-backend-auditoria-compliance]
```

### **Servidor Hostinger**
- **Host:** srv649924
- **Serviço:** `conexao-de-sorte-backend-auditoria-compliance`
- **Status:** Verificar com `systemctl status conexao-de-sorte-backend-auditoria-compliance`

## 🚀 Deploy e Execução

### **Local Development**
```bash
# Configurar variáveis de ambiente
cp .env.example .env
# Editar .env com valores locais

# Executar com Docker Compose
docker-compose up -d

# Verificar saúde
curl http://localhost:8085/actuator/health
```

### **Staging/Production**
```bash
# Deploy via GitHub Actions (automático)
# Trigger: push para main ou workflow_dispatch

# Verificar deploy
curl http://srv649924:8085/actuator/health
```

## 🔍 Monitoramento

### **Health Checks**
- **Endpoint:** `/actuator/health`
- **Porta:** 8085
- **Timeout:** 10s
- **Intervalo:** 30s

### **Métricas**
- **Prometheus:** `/actuator/prometheus`
- **Grafana:** Integrado com backend-observabilidade
- **Jaeger:** Tracing distribuído habilitado

## 🛠️ Desenvolvimento

### **Pré-requisitos**
- Java 21 LTS
- Maven 3.9+
- Docker & Docker Compose
- Azure CLI (para secrets)

### **Build Local**
```bash
# Compilar
./mvnw clean compile

# Testes (requer configuração DB)
./mvnw test

# Build Docker
docker build -t auditoria-compliance:local .
```

### **Estrutura do Projeto**
```
src/
├── main/java/br/tec/facilitaservicos/auditoria/
│   ├── aplicacao/          # Casos de uso e serviços
│   ├── dominio/           # Entidades e repositórios
│   ├── infraestrutura/    # Configurações e adaptadores
│   └── apresentacao/      # Controllers REST
└── test/                  # Testes unitários e integração
```

## 📋 Compliance

### **LGPD**
- ✅ Retenção de dados configurável
- ✅ Anonimização automática
- ✅ Trilha de consentimento
- ✅ Relatórios de conformidade

### **SOX**
- ✅ Controles financeiros
- ✅ Trilha de auditoria imutável
- ✅ Segregação de funções
- ✅ Validação criptográfica

### **Segurança**
- ✅ OIDC Authentication
- ✅ Secrets via Azure Key Vault
- ✅ Container não-root
- ✅ Scan de vulnerabilidades

## 🔧 Troubleshooting

### **Problemas Comuns**

#### **1. Falha na Autenticação Azure**
```bash
# Verificar OIDC
az account show

# Verificar Key Vault
az keyvault secret list --vault-name <vault-name>
```

#### **2. Container não inicia**
```bash
# Verificar logs
docker logs auditoria-service

# Verificar secrets
docker exec auditoria-service env | grep -E "(DATABASE|REDIS|JWT)"
```

#### **3. Health Check falha**
```bash
# Verificar conectividade
curl -v http://localhost:8085/actuator/health

# Verificar dependências
docker network ls | grep conexao
```

## 📚 Documentação Adicional

- [Mapa de Uso de Segredos](docs/secrets-usage-map.md)
- [Checklist de Pipeline](docs/pipeline-checklist.md)
- [Configuração de Segredos](docs/SECRETS_CONFIGURATION.md)
- [Validação de Integração](VALIDACAO_INTEGRACAO_COMPLETA.md)

## 🔄 CI/CD Pipeline

### **Workflow Triggers**
- Push para qualquer branch
- Pull Request
- Manual dispatch

### **Etapas**
1. **Checkout** - actions/checkout@v4
2. **Azure Login** - OIDC authentication
3. **Key Vault** - Busca seletiva de 12 segredos
4. **Build** - Maven compile + tests
5. **Docker** - Build + security scan
6. **Deploy** - Staging/production

### **Validação**
- ✅ Actionlint (workflow syntax)
- ✅ Maven build (compile)
- ⚠️ Tests (configuração DB pendente)
- ✅ Trivy scan (vulnerabilidades)
- ✅ Health check (deploy)

---

**Última atualização:** $(date '+%Y-%m-%d %H:%M:%S')
**Status:** ✅ **Auditoria Completa** - Conforme com todos os critérios de segurança

## 🎯 Características Principais

- **📋 Event Sourcing**: Axon Framework para auditoria imutável
- **🚀 100% Reativo**: Spring WebFlux + R2DBC para máxima performance
- **🔐 Segurança JWT**: Validação via JWKS do microserviço de autenticação
- **🔒 WORM Storage**: Write-Once, Read-Many para integridade
- **🔗 Cadeia de Hashes**: Integridade criptográfica encadeada
- **✍️ Assinaturas Digitais**: Azure Key Vault + BouncyCastle
- **📊 CQRS**: Consultas otimizadas com read models
- **📡 Event Streaming**: Kafka para eventos distribuídos
- **🛡️ GDPR Compliance**: Right to be forgotten + minimização PII
- **📈 Relatórios**: Compliance automático com métricas
- **⚡ Cache Inteligente**: Redis para performance de consultas
- **🐳 Containerizado**: Docker + Docker Compose
- **📊 API Documentada**: OpenAPI 3 + Swagger UI
- **🧪 Testado**: Testes unitários e de integração
- **🔄 Anti-extração**: Mantém funcionalidade no monólito

## 🏗️ Arquitetura

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │◄──▶│ Microserviço     │───▶│   PostgreSQL    │
│   (REST API)    │    │   Auditoria      │    │  (Event Store)  │
│                 │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │                         │
                              ▼                         ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │   Kafka          │    │   MySQL         │
                       │   (Events)       │    │  (Read Models)  │
                       └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │   Azure Key      │
                       │   Vault          │
                       │   (Signing)      │
                       └──────────────────┘
```

## 🛠️ Stack Tecnológica

- **Java 24** - Linguagem principal
- **Spring Boot 3.5+** - Framework base
- **Spring WebFlux** - Programação reativa
- **Spring Security** - Segurança JWT
- **R2DBC PostgreSQL** - Event store otimizado
- **Axon Framework** - Event Sourcing + CQRS
- **Apache Kafka** - Event streaming distribuído
- **BouncyCastle** - Criptografia e assinaturas
- **Azure Key Vault** - Gerenciamento de chaves
- **Redis** - Cache + Read models
- **Flyway** - Migrations de banco
- **Docker** - Containerização
- **Testcontainers** - Testes de integração

## 🚀 Início Rápido

### Pré-requisitos

- Java 24+
- Docker e Docker Compose
- Maven 3.9+
- Azure Key Vault (para produção)

### 1. Clone e Execute

```bash
# Clone o projeto
cd /Volumes/NVME/Projetos/conexao-de-sorte-backend-auditoria-compliance

# Execute com Docker Compose
docker-compose up -d

# Ou execute localmente
mvn spring-boot:run
```

### 2. Acesse os Serviços

- **API**: http://localhost:8085/rest/v1/auditoria
- **Swagger UI**: http://localhost:8085/swagger-ui.html
- **Actuator**: http://localhost:8085/actuator
- **Kafka Control Center**: http://localhost:9021
- **Grafana**: http://localhost:3004 (admin:admin123!)
- **Prometheus**: http://localhost:9094

## 📋 Endpoints da API

### Consultas de Auditoria

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/rest/v1/auditoria/eventos` | Consultar eventos com filtros |
| `GET` | `/rest/v1/auditoria/trilha/{entidade}/{id}` | Trilha de uma entidade |
| `GET` | `/rest/v1/auditoria/correlacao/{correlationId}` | Eventos por correlação |
| `GET` | `/rest/v1/auditoria/integridade/{eventoId}` | Verificar integridade |

### Compliance e Relatórios

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/rest/v1/auditoria/relatorio/compliance` | Relatório de compliance |
| `GET` | `/rest/v1/auditoria/metricas` | Métricas de auditoria |
| `GET` | `/rest/v1/auditoria/estatisticas/integridade` | Estatísticas de integridade |

### Verificações Criptográficas

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/rest/v1/auditoria/verificar-assinatura` | Verificar assinatura digital |
| `POST` | `/rest/v1/auditoria/validar-cadeia` | Validar cadeia de hashes |

## 🎮 Exemplos de Uso

### Consultar Eventos de Auditoria

```bash
curl -X GET "http://localhost:8085/rest/v1/auditoria/eventos?tipoEvento=LOGIN&dataInicio=2024-01-01T00:00:00&dataFim=2024-12-31T23:59:59&page=0&size=50" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Obter Trilha de uma Entidade

```bash
curl -X GET "http://localhost:8085/rest/v1/auditoria/trilha/usuario/123" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Verificar Integridade de Evento

```bash
curl -X GET "http://localhost:8085/rest/v1/auditoria/integridade/evento-uuid-123" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Gerar Relatório de Compliance

```bash
curl -X GET "http://localhost:8085/rest/v1/auditoria/relatorio/compliance?dataInicio=2024-01-01T00:00:00&dataFim=2024-12-31T23:59:59&tipoRelatorio=GDPR" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Verificar Assinatura Digital

```bash
curl -X POST "http://localhost:8085/rest/v1/auditoria/verificar-assinatura" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventoId": "evento-uuid-123",
    "assinatura": "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSk..."
  }'
```

## 🔧 Configuração

### Variáveis de Ambiente

```bash
# Database (Event Store)
EVENT_STORE_HOST=localhost
EVENT_STORE_PORT=5432
EVENT_STORE_DB=conexao_sorte_events
EVENT_STORE_USERNAME=auditoria_user
EVENT_STORE_PASSWORD=auditoria_pass123!

# Database (Read Models)
DB_HOST=localhost
DB_PORT=3310
DB_NAME=conexao_sorte_auditoria
DB_USERNAME=auditoria_user
DB_PASSWORD=auditoria_pass123!

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_SECURITY_PROTOCOL=PLAINTEXT

# Redis
REDIS_HOST=localhost
REDIS_PORT=6383
REDIS_PASSWORD=redis_pass123!
REDIS_DB=5

# JWT
JWT_JWKS_URI=http://localhost:8081/.well-known/jwks.json
JWT_ISSUER=https://auth.conexaodesorte.com

# Azure Key Vault
AZURE_KEYVAULT_URI=https://conexao-sorte-kv.vault.azure.net/
AZURE_CLIENT_ID=your-client-id
# OIDC-only: não utilizar AZURE_CLIENT_SECRET; use OIDC + Key Vault
AZURE_TENANT_ID=your-tenant-id

# Features
FEATURE_AUDITORIA_MS=true
FEATURE_EVENT_SOURCING=true
FEATURE_DIGITAL_SIGNATURES=true
FEATURE_COMPLIANCE_REPORTS=true
FEATURE_GDPR_COMPLIANCE=true
```

### Configuração de Auditoria

```yaml
auditoria:
  event-sourcing:
    snapshot-frequency: 1000
    replay-batch-size: 500
    retention-days: 2555  # 7 anos
  integridade:
    hash-algorithm: SHA-256
    signature-algorithm: RSA-PSS
    chain-validation: true
  compliance:
    gdpr-enabled: true
    data-retention-days: 2555
    anonymization-enabled: true
    export-formats: [JSON, CSV, XML]
  performance:
    read-model-refresh: 5m
    cache-ttl: 1h
    batch-size: 1000
```

## 🧪 Testes

```bash
# Testes unitários
mvn test

# Testes de integração
mvn verify

# Testes com Testcontainers
mvn test -Dtest=**/*IntegrationTest

# Teste de performance
mvn test -Dtest=**/*PerformanceTest
```

## 📊 Monitoramento

### Métricas Customizadas

- `auditoria.eventos.total` - Total de eventos processados
- `auditoria.integridade.verificacoes` - Verificações de integridade
- `auditoria.assinaturas.validas` - Assinaturas digitais válidas
- `auditoria.compliance.violacoes` - Violações de compliance
- `auditoria.gdpr.solicitacoes` - Solicitações GDPR processadas

### Health Checks

- **Event Store**: Conectividade PostgreSQL
- **Kafka**: Produção + Consumo de eventos
- **Azure Key Vault**: Acesso às chaves
- **Redis**: Cache disponível
- **Cadeia de Integridade**: Validação contínua

## 🚀 Performance

### Características de Performance

- **Event Store Otimizado**: PostgreSQL com particionamento temporal
- **CQRS Read Models**: Consultas pré-calculadas em Redis
- **Streaming Assíncrono**: Kafka para eventos distribuídos
- **Snapshots Inteligentes**: Reconstrução otimizada de agregados
- **Connection Pooling**: R2DBC otimizado
- **Batch Processing**: Processamento em lotes eficiente

### Configurações de Produção

```yaml
# Performance otimizada
spring:
  r2dbc:
    pool:
      initial-size: 30
      max-size: 150

kafka:
  producer:
    batch-size: 65536
    linger-ms: 5
  consumer:
    max-poll-records: 1000

axon:
  eventhandling:
    processors:
      default:
        mode: tracking
        batch-size: 1000
```

## 🔐 Segurança e Compliance

### Integridade Criptográfica

- **Hash Encadeado**: SHA-256 com previous hash
- **Assinaturas Digitais**: RSA-PSS com Azure Key Vault
- **Timestamps**: RFC 3161 para não-repúdio
- **Imutabilidade**: WORM storage garantido

### GDPR Compliance

- **Right to be Forgotten**: Anonimização segura
- **Data Minimization**: Mascaramento automático de PII
- **Consent Tracking**: Trilha de consentimentos
- **Data Export**: Formatos padronizados (JSON, XML)
- **Breach Notification**: Alertas automáticos

### Auditoria de Segurança

```json
{
  "tipoEvento": "ACESSO_DADOS_PESSOAIS",
  "entidade": "usuario",
  "entidadeId": "123",
  "dadosAcessados": ["email", "telefone"],
  "justificativa": "Processamento de pedido",
  "baseLegal": "legitimate_interest",
  "consentimento": "consent-uuid-456",
  "hash": "a8b94f5e2c...",
  "assinatura": "MIIEvgIBADAN..."
}
```

## 📋 Tipos de Eventos de Auditoria

### Autenticação e Autorização
- `LOGIN_SUCESSO` / `LOGIN_FALHA`
- `LOGOUT`
- `ALTERACAO_SENHA`
- `TENTATIVA_ACESSO_NAO_AUTORIZADO`

### Dados Pessoais (GDPR)
- `ACESSO_DADOS_PESSOAIS`
- `MODIFICACAO_DADOS_PESSOAIS`
- `EXPORTACAO_DADOS`
- `SOLICITACAO_ESQUECIMENTO`
- `ANONIMIZACAO_DADOS`

### Transações Financeiras
- `TRANSACAO_CRIADA`
- `TRANSACAO_PROCESSADA`
- `TRANSACAO_CANCELADA`
- `ALTERACAO_LIMITE`

### Sistema
- `CONFIGURACAO_ALTERADA`
- `BACKUP_EXECUTADO`
- `RESTAURACAO_EXECUTADA`
- `MANUTENCAO_SISTEMA`

## 🐳 Docker

### Build Local

```bash
# Build da imagem
docker build -t auditoria-microservice .

# Executar container
docker run -p 8085:8085 \
  -e FEATURE_AUDITORIA_MS=true \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  auditoria-microservice
```

### Docker Compose Completo

```bash
# Subir ambiente completo
docker-compose up -d

# Ver logs em tempo real
docker-compose logs -f auditoria-service

# Parar ambiente
docker-compose down
```

## 🔄 Integração com Monólito

- **Feature Flag**: `FEATURE_AUDITORIA_MS=false` por padrão
- **Anti-extração**: Funcionalidade preservada no monólito
- **Event Bridge**: Sincronização bidirecional de eventos
- **Rollback Seguro**: Desativação instantânea

## 🎯 Casos de Uso

### Auditoria Financeira
- Trilha completa de transações
- Verificação de integridade contábil
- Relatórios de compliance fiscal

### GDPR e Privacidade
- Registro de consentimentos
- Trilha de acesso a dados pessoais
- Relatórios de compliance LGPD/GDPR

### Segurança e Forense
- Investigação de incidentes
- Análise de padrões suspeitos
- Evidências digitais válidas

### Compliance Regulatório
- SOX, PCI-DSS, ISO 27001
- Auditoria contínua
- Relatórios automáticos

## 🤝 Contribuição

1. Clone o repositório
2. Crie uma branch: `git checkout -b feature/nova-funcionalidade`
3. Faça commit: `git commit -m 'Adiciona novo tipo de evento'`
4. Push: `git push origin feature/nova-funcionalidade`
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para detalhes.

---

**📋 Microserviço Auditoria & Compliance** - Sistema de Migração R2DBC v1.0
## ✅ Qualidade e Segurança (CI)

- Cobertura: JaCoCo ≥ 80% (gate no workflow Maven Verify).
- SAST: CodeQL habilitado para varredura contínua.

## 🧪 Staging: Integrações, Robustez e Cache

- Integrações Kafka/PostgreSQL: validar conectividade e tópicos/DDL no ambiente de Staging.
- Event Sourcing: validar fluxo e integridade dos eventos (replay/consistência).
- Resilience4j: validar circuit breakers e retries em falhas simuladas.
- Cache: validar estratégia/TTLs para consultas de auditoria/compliance.
