# 📋 Guia do Projeto: Auditoria e Compliance
## Microserviço de Auditoria e Conformidade Regulatória

> **🎯 Contexto**: Microserviço responsável pelo logging de auditoria, conformidade LGPD, rastreamento de eventos críticos e geração de relatórios de compliance para a plataforma.

---

## 📋 INFORMAÇÕES DO PROJETO

### **Identificação:**
- **Nome**: conexao-de-sorte-backend-auditoria-compliance
- **Porta**: 8082
- **Rede Principal**: conexao-network-swarm
- **Database**: conexao_auditoria_compliance (MySQL 8.4)
- **Runner**: `[self-hosted, Linux, X64, conexao, conexao-de-sorte-backend-auditoria-compliance]`

### **Tecnologias Específicas:**
- Spring Boot 3.5.5 + Spring WebFlux (reativo)
- R2DBC MySQL (persistência reativa)
- Redis (cache de relatórios)
- Event Sourcing (CQRS pattern)
- Kafka/RabbitMQ (processamento assíncrono)

---

## 🗄️ ESTRUTURA DO BANCO DE DADOS

### **Database**: `conexao_auditoria_compliance`

#### **Tabelas:**
1. **`eventos_auditoria`** - Eventos de auditoria
2. **`logs_lgpd`** - Logs específicos LGPD
3. **`relatorios_compliance`** - Relatórios gerados

#### **Estrutura das Tabelas:**
```sql
-- eventos_auditoria
id (String PK)
timestamp (DateTime)
usuario_id (String)              -- Referência para autenticacao.usuarios.id
servico_origem (String)          -- Nome do microserviço
tipo_evento (String)             -- LOGIN, TRANSACAO, ACESSO_DADOS, etc.
acao (String)                    -- CREATE, READ, UPDATE, DELETE
recurso (String)                 -- Recurso acessado
ip_origem (String)
user_agent (String)
session_id (String)
resultado (String)               -- SUCCESS, FAILURE, BLOCKED
detalhes (JSON)                  -- Dados adicionais do evento
nivel_criticidade (String)       -- LOW, MEDIUM, HIGH, CRITICAL
tags (JSON)                      -- Tags para categorização
criado_em (DateTime)

-- logs_lgpd  
id (String PK)
usuario_id (String)              -- Referência para autenticacao.usuarios.id
tipo_processamento (String)      -- COLETA, TRATAMENTO, COMPARTILHAMENTO
base_legal (String)              -- Artigo da LGPD
finalidade (String)
dados_tratados (JSON)
consentimento_id (String)
data_consentimento (DateTime)
data_revogacao (DateTime)
retenção_ate (DateTime)
anonimizado (Boolean)
criado_em (DateTime)

-- relatorios_compliance
id (String PK)
tipo_relatorio (String)          -- LGPD, AUDITORIA, SEGURANCA
periodo_inicio (Date)
periodo_fim (Date)
parametros (JSON)
status (String)                  -- PENDING, PROCESSING, COMPLETED, FAILED
arquivo_gerado (String)          -- Path do arquivo
hash_integridade (String)
criado_por (String)
criado_em (DateTime)
finalizado_em (DateTime)
```

#### **Relacionamentos Inter-Serviços:**
- eventos_auditoria.usuario_id → autenticacao.usuarios.id
- logs_lgpd.usuario_id → autenticacao.usuarios.id

### **Configuração R2DBC:**
```yaml
r2dbc:
  url: r2dbc:mysql://mysql-proxy:6033/conexao_auditoria_compliance
  pool:
    initial-size: 2
    max-size: 20
```

---

## 🔐 SECRETS ESPECÍFICOS

### **Azure Key Vault Secrets Utilizados:**
```yaml
# Database
conexao-de-sorte-database-r2dbc-url
conexao-de-sorte-database-username
conexao-de-sorte-database-password

# Redis Cache
conexao-de-sorte-redis-host
conexao-de-sorte-redis-password
conexao-de-sorte-redis-port

# JWT for service-to-service
conexao-de-sorte-jwt-secret
conexao-de-sorte-jwt-verification-key

# Messaging (Kafka/RabbitMQ)
conexao-de-sorte-messaging-host
conexao-de-sorte-messaging-username
conexao-de-sorte-messaging-password

# LGPD Compliance
conexao-de-sorte-lgpd-encryption-key
conexao-de-sorte-anonymization-key
conexao-de-sorte-audit-retention-days

# Report Generation
conexao-de-sorte-report-signing-key
conexao-de-sorte-report-storage-path
```

### **Cache Redis Específico:**
```yaml
redis:
  database: 7
  cache-names:
    - audit:events
    - audit:reports
    - compliance:lgpd
    - audit:users-activity
```

---

## 🌐 INTEGRAÇÃO DE REDE

### **Comunicação Entrada (Server):**
- **Todos os microserviços** → Auditoria (eventos de auditoria)
- **Gateway** → Auditoria (rotas /api/auditoria/*)
- **Admin Interface** → Auditoria (relatórios e consultas)
- **Message Queue** → Auditoria (eventos assíncronos)

### **Comunicação Saída (Client):**
- Auditoria → **Autenticação** (validação JWT + dados usuário)
- Auditoria → **Message Queue** (publicação eventos críticos)
- Auditoria → **Storage** (arquivamento relatórios)
- Auditoria → **Email Service** (notificações compliance)

### **Portas e Endpoints:**
```yaml
server.port: 8082

# Endpoints principais:
POST   /auditoria/eventos          # Receber eventos
GET    /auditoria/eventos          # Consultar eventos
GET    /auditoria/usuario/{id}     # Auditoria por usuário
POST   /auditoria/relatorio        # Gerar relatório
GET    /auditoria/relatorio/{id}   # Download relatório

# LGPD Endpoints
GET    /lgpd/dados-usuario/{id}    # Dados pessoais do usuário
POST   /lgpd/anonimizar/{id}       # Anonimizar dados
POST   /lgpd/excluir/{id}          # Direito ao esquecimento
GET    /lgpd/consentimentos/{id}   # Histórico consentimentos

# Compliance Endpoints  
GET    /compliance/dashboard       # Dashboard compliance
GET    /compliance/metricas        # Métricas de conformidade
POST   /compliance/notificacao     # Notificar violação

GET    /actuator/health
```

---

## 🔗 DEPENDÊNCIAS CRÍTICAS

### **Serviços Dependentes (Upstream):**
1. **MySQL** (mysql-proxy:6033) - Persistência principal
2. **Redis** (conexao-redis:6379) - Cache de relatórios
3. **Autenticação** (8081) - Dados de usuários
4. **Message Queue** - Processamento assíncrono
5. **Azure Key Vault** - Secrets management

### **Serviços Consumidores (Downstream):**
- **Todos os microserviços** - Envio de eventos
- **Admin Interface** - Relatórios
- **Órgãos Reguladores** - Relatórios LGPD
- **Auditoria Externa** - Logs de auditoria

### **Ordem de Deploy:**
```
1. MySQL + Redis (infrastructure)
2. Message Queue (Kafka/RabbitMQ)
3. Autenticação (dados usuário)
4. Auditoria (logging service)
5. Demais microserviços (event sources)
```

---

## 🚨 ESPECIFICIDADES DA AUDITORIA

### **Tipos de Eventos Auditados:**
```yaml
eventos-criticos:
  - LOGIN_FAILURE
  - PRIVILEGE_ESCALATION  
  - DATA_EXPORT
  - FINANCIAL_TRANSACTION
  - ADMIN_ACTION
  - GDPR_REQUEST
  
eventos-business:
  - USER_REGISTRATION
  - GAME_RESULT_VIEW
  - NOTIFICATION_SENT
  - CHAT_MESSAGE
```

### **Retenção de Dados:**
```yaml
retencao:
  eventos-auditoria: 7-anos
  logs-lgpd: 5-anos
  relatorios: permanente
  dados-sensiveis: criptografados
```

### **Processamento LGPD:**
```yaml
lgpd:
  anonimizacao-automatica: 30-dias-inatividade
  direito-esquecimento: 15-dias-processamento
  portabilidade-dados: json/xml-export
  consentimento-granular: por-finalidade
```

---

## 📊 MÉTRICAS ESPECÍFICAS

### **Custom Metrics:**
- `audit_eventos_total{servico,tipo,resultado}` - Eventos por tipo
- `audit_eventos_criticos_total` - Eventos críticos
- `audit_lgpd_requests_total{tipo}` - Requests LGPD
- `audit_relatorios_gerados_total` - Relatórios gerados
- `audit_data_retention_violations` - Violações de retenção
- `audit_processing_duration{tipo}` - Tempo de processamento

### **Alertas Configurados:**
- Eventos críticos > 10/min
- Falhas LGPD processing > 1%
- Data retention violations > 0
- Report generation failures > 5%
- Disk space reports < 20%
- Event processing lag > 5min

---

## 🔧 CONFIGURAÇÕES ESPECÍFICAS

### **Application Properties:**
```yaml
# Auditoria Configuration
auditoria:
  buffer-size: 1000
  flush-interval: 30s
  retention-days: ${conexao-de-sorte-audit-retention-days:2557} # 7 anos
  critical-events-immediate-flush: true
  
# LGPD Configuration
lgpd:
  anonymization:
    enabled: true
    auto-trigger-days: 30
    encryption-key: ${conexao-de-sorte-anonymization-key}
  data-retention:
    user-data-days: 1825  # 5 anos
    financial-data-days: 2557  # 7 anos
  right-to-be-forgotten:
    processing-days: 15
    
# Report Generation
reports:
  storage-path: ${conexao-de-sorte-report-storage-path}
  signing-enabled: true
  signing-key: ${conexao-de-sorte-report-signing-key}
  formats: [PDF, JSON, XML, CSV]
  
# Event Processing
events:
  async-processing: true
  batch-size: 100
  max-retry-attempts: 3
  dead-letter-queue: audit-dlq
```

### **Compliance Rules:**
```yaml
compliance:
  lgpd:
    data-controller: "Conexão de Sorte Ltda"
    dpo-contact: "dpo@conexaodesorte.com.br"
    legal-basis-mapping:
      user-registration: "consent"
      financial-transaction: "contract"
      security-audit: "legitimate-interest"
```

---

## 🧪 TESTES E VALIDAÇÕES

### **Health Checks:**
```bash
# Health principal
curl -f http://localhost:8082/actuator/health

# Database connectivity
curl -f http://localhost:8082/actuator/health/db

# Redis connectivity
curl -f http://localhost:8082/actuator/health/redis

# Event processing
curl -f http://localhost:8082/auditoria/health/events
```

### **Smoke Tests Pós-Deploy:**
```bash
# 1. Enviar evento de auditoria
curl -X POST http://localhost:8082/auditoria/eventos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT" \
  -d '{
    "tipo_evento": "TEST_EVENT",
    "acao": "READ",
    "recurso": "test-resource",
    "usuario_id": "test-user"
  }'

# 2. Gerar relatório teste
curl -X POST http://localhost:8082/auditoria/relatorio \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT" \
  -d '{
    "tipo_relatorio": "AUDITORIA",
    "periodo_inicio": "2024-01-01",
    "periodo_fim": "2024-12-31"
  }'

# 3. Testar LGPD endpoint
curl -H "Authorization: Bearer $ADMIN_JWT" \
  http://localhost:8082/lgpd/dados-usuario/test-user-id
```

---

## ⚠️ TROUBLESHOOTING

### **Problema: Eventos Perdidos**
```bash
# 1. Verificar buffer auditoria
curl http://localhost:8082/actuator/metrics/audit.buffer.size

# 2. Verificar message queue
# Para Kafka:
kafka-console-consumer --bootstrap-server localhost:9092 --topic audit-events

# 3. Verificar logs processamento
docker service logs conexao-auditoria | grep "event processing"
```

### **Problema: Relatórios Não Geram**
```bash
# 1. Verificar espaço em disco
df -h | grep reports

# 2. Verificar permissões diretório
ls -la ${REPORT_STORAGE_PATH}

# 3. Verificar status relatórios
curl http://localhost:8082/auditoria/relatorio/status
```

### **Problema: LGPD Processing Falha**
```bash
# 1. Verificar chave anonimização
az keyvault secret show --vault-name kv-conexao-de-sorte --name conexao-de-sorte-anonymization-key

# 2. Verificar logs LGPD
docker service logs conexao-auditoria | grep "LGPD"

# 3. Verificar queue LGPD
redis-cli -a $REDIS_PASS LLEN "lgpd:processing"
```

---

## 📋 CHECKLIST PRÉ-DEPLOY

### **Configuração:**
- [ ] Database `conexao_auditoria_compliance` criado
- [ ] Redis cache configurado (database 7)
- [ ] Message queue configurado
- [ ] JWT secrets no Key Vault
- [ ] Chaves LGPD configuradas

### **Compliance:**
- [ ] Mapeamento base legal LGPD configurado
- [ ] Política de retenção configurada
- [ ] Processo anonimização testado
- [ ] Relatórios compliance validados

### **Integração:**
- [ ] Todos microserviços enviando eventos
- [ ] Admin interface conectada
- [ ] Storage relatórios configurado
- [ ] Notificações compliance ativas

---

## 🔄 DISASTER RECOVERY

### **Backup Crítico:**
1. **Database auditoria** (CRÍTICO - compliance legal)
2. **Relatórios gerados** (permanente)
3. **Chaves de anonimização**
4. **Configurações LGPD**

### **Recovery Procedure:**
1. Restore database auditoria (prioridade máxima)
2. Restore report storage
3. Restart audit service
4. Verify LGPD processing
5. Test report generation
6. Validate event ingestion
7. Check compliance dashboard

### **RTO/RPO:**
- **RTO**: < 1 hora (compliance crítico)
- **RPO**: < 5 minutos (eventos não podem ser perdidos)

---

## 💡 OPERATIONAL NOTES

### **Compliance Crítico:**
- **Retenção legal**: 7 anos eventos financeiros
- **LGPD**: Processamento em 15 dias
- **Auditoria**: Logs imutáveis
- **Relatórios**: Assinatura digital

### **Performance:**
- Event ingestion: > 1000 events/sec
- Report generation: < 5 min (padrão)
- LGPD processing: < 15 dias (legal)
- Storage growth: ~1GB/mês

### **Monitoramento 24/7:**
- Event processing lag
- LGPD compliance SLA
- Report generation success
- Storage capacity
- Critical event alerts

---

**📅 Última Atualização**: Setembro 2025  
**🏷️ Versão**: 1.0  
**📋 Criticidade**: CRÍTICA - Compliance legal obrigatória