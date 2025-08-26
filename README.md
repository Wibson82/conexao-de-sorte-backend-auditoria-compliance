# 📋 Microserviço de Auditoria & Compliance

[![Java](https://img.shields.io/badge/Java-24-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-green.svg)](https://spring.io/projects/spring-boot)
[![WebFlux](https://img.shields.io/badge/WebFlux-Reactive-blue.svg)](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
[![R2DBC](https://img.shields.io/badge/R2DBC-Reactive-purple.svg)](https://r2dbc.io/)
[![Event Sourcing](https://img.shields.io/badge/Event%20Sourcing-Axon-green.svg)](https://axoniq.io/)
[![WORM Storage](https://img.shields.io/badge/WORM-Storage-red.svg)](https://en.wikipedia.org/wiki/Write_once_read_many)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

Microserviço **100% reativo** para auditoria e compliance, construído com Spring WebFlux, Event Sourcing (Axon), WORM storage e trilhas imutáveis de auditoria.

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

- **API**: http://localhost:8085/api/auditoria
- **Swagger UI**: http://localhost:8085/swagger-ui.html
- **Actuator**: http://localhost:8085/actuator
- **Kafka Control Center**: http://localhost:9021
- **Grafana**: http://localhost:3004 (admin:admin123!)
- **Prometheus**: http://localhost:9094

## 📋 Endpoints da API

### Consultas de Auditoria

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/auditoria/eventos` | Consultar eventos com filtros |
| `GET` | `/api/auditoria/trilha/{entidade}/{id}` | Trilha de uma entidade |
| `GET` | `/api/auditoria/correlacao/{correlationId}` | Eventos por correlação |
| `GET` | `/api/auditoria/integridade/{eventoId}` | Verificar integridade |

### Compliance e Relatórios

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/auditoria/relatorio/compliance` | Relatório de compliance |
| `GET` | `/api/auditoria/metricas` | Métricas de auditoria |
| `GET` | `/api/auditoria/estatisticas/integridade` | Estatísticas de integridade |

### Verificações Criptográficas

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/auditoria/verificar-assinatura` | Verificar assinatura digital |
| `POST` | `/api/auditoria/validar-cadeia` | Validar cadeia de hashes |

## 🎮 Exemplos de Uso

### Consultar Eventos de Auditoria

```bash
curl -X GET "http://localhost:8085/api/auditoria/eventos?tipoEvento=LOGIN&dataInicio=2024-01-01T00:00:00&dataFim=2024-12-31T23:59:59&page=0&size=50" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Obter Trilha de uma Entidade

```bash
curl -X GET "http://localhost:8085/api/auditoria/trilha/usuario/123" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Verificar Integridade de Evento

```bash
curl -X GET "http://localhost:8085/api/auditoria/integridade/evento-uuid-123" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Gerar Relatório de Compliance

```bash
curl -X GET "http://localhost:8085/api/auditoria/relatorio/compliance?dataInicio=2024-01-01T00:00:00&dataFim=2024-12-31T23:59:59&tipoRelatorio=GDPR" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Verificar Assinatura Digital

```bash
curl -X POST "http://localhost:8085/api/auditoria/verificar-assinatura" \
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
AZURE_CLIENT_SECRET=your-client-secret
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