# 🔐 Configuração de Segredos - Auditoria & Compliance Microservice

## 📋 Resumo
Este documento lista todos os segredos necessários para o funcionamento correto do microserviço de auditoria e compliance, organizados por categoria e local de configuração.

## 🔧 Segredos do GitHub Actions

### Configuração no GitHub Repository > Settings > Secrets and variables > Actions

```env
# Azure Authentication & Infrastructure
AZURE_CLIENT_ID=<azure-client-id>
AZURE_TENANT_ID=<azure-tenant-id>  
AZURE_SUBSCRIPTION_ID=<azure-subscription-id>
AZURE_KEYVAULT_ENDPOINT=https://vault-name.vault.azure.net/
AZURE_KEYVAULT_NAME=vault-name

# GitHub Container Registry (Automatically available)
GITHUB_TOKEN=<automatically-provided-by-github>

# Optional: Slack Notifications
SLACK_WEBHOOK_URL=<slack-webhook-url>
```

## 🏗️ Segredos do Azure Key Vault

### Configuração no Azure Key Vault especificado em `AZURE_KEYVAULT_ENDPOINT`

#### 🗄️ Database Configuration
```env
conexao-de-sorte-database-r2dbc-url=r2dbc:mysql://host:port/database
conexao-de-sorte-database-username=username
conexao-de-sorte-database-password=password
conexao-de-sorte-database-jdbc-url=jdbc:mysql://host:port/database
conexao-de-sorte-database-url=jdbc:mysql://host:port/database
```

#### 🔄 Redis Configuration
```env
conexao-de-sorte-redis-host=redis-host
conexao-de-sorte-redis-port=6379
conexao-de-sorte-redis-password=redis-password
conexao-de-sorte-redis-database=0
```

#### 🔑 JWT Configuration
```env
conexao-de-sorte-jwt-secret=jwt-secret-key
conexao-de-sorte-jwt-key-id=jwt-key-identifier
conexao-de-sorte-jwt-signing-key=jwt-signing-key
conexao-de-sorte-jwt-verification-key=jwt-verification-key
conexao-de-sorte-jwt-issuer=https://auth.conexaodesorte.com
conexao-de-sorte-jwt-jwks-uri=https://auth.conexaodesorte.com/.well-known/jwks.json
conexao-de-sorte-jwt-privateKey=-----BEGIN PRIVATE KEY-----...
conexao-de-sorte-jwt-publicKey=-----BEGIN PUBLIC KEY-----...
```

#### 🔐 Encryption Configuration
```env
conexao-de-sorte-encryption-master-key=master-encryption-key
conexao-de-sorte-encryption-master-password=master-encryption-password
conexao-de-sorte-encryption-backup-key=backup-encryption-key
```

#### 🌐 SSL/TLS Configuration
```env
conexao-de-sorte-ssl-enabled=true
conexao-de-sorte-ssl-keystore-path=/path/to/keystore.p12
conexao-de-sorte-ssl-keystore-password=keystore-password
```

#### 🔄 CORS Configuration
```env
conexao-de-sorte-cors-allowed-origins=https://conexaodesorte.com.br,https://app.conexaodesorte.com.br
conexao-de-sorte-cors-allow-credentials=true
```

## 📁 Estrutura de Arquivos de Segredos no Container

Durante o deployment, os segredos são montados no container em `/run/secrets/`:

```
/run/secrets/
├── jwt_private.pem          # Private key JWT (de conexao-de-sorte-jwt-privateKey)
├── jwt_public.pem           # Public key JWT (de conexao-de-sorte-jwt-publicKey)  
├── encryption_master_key    # Master encryption key
├── encryption_master_password # Master encryption password
└── encryption_backup_key    # Backup encryption key
```

## 🔒 Segurança e Melhores Práticas

### ✅ Implementadas
- **OIDC Authentication**: Autenticação sem armazenar credenciais
- **Azure Key Vault**: Armazenamento centralizado de segredos
- **Configtree**: Leitura segura de arquivos de secrets
- **Volume Read-Only**: Secrets montados como read-only no container
- **Cleanup Automático**: Remoção de arquivos temporários após deployment
- **Princípio do Menor Privilégio**: Apenas secrets necessários são expostos

### 🚀 Configuração Spring Boot

O Spring Boot usa automaticamente os segredos através de:

1. **Configtree**: `SPRING_CONFIG_IMPORT=optional:configtree:/run/secrets/`
2. **Environment Variables**: Variáveis com prefix `conexao_de_sorte_`
3. **Azure Key Vault**: Integração nativa com Spring Cloud Azure

### 📊 Monitoramento

O pipeline monitora:
- ✅ Availability dos segredos no Azure Key Vault
- ✅ Connectivity com Azure via OIDC
- ✅ Health checks da aplicação após deployment
- ✅ Cleanup de arquivos temporários

## 🛠️ Troubleshooting

### Erros Comuns

1. **Secret não encontrado no Azure Key Vault**
   ```bash
   ERROR: Secret conexao-de-sorte-xxx not found in Azure Key Vault
   ```
   **Solução**: Verificar se o secret existe no Key Vault correto

2. **OIDC Authentication Failed**
   ```bash
   ERROR: Invalid AZURE_KEYVAULT_ENDPOINT format
   ```
   **Solução**: Verificar formato `https://vault-name.vault.azure.net/`

3. **Container falha no health check**
   ```bash
   Service failed to become ready within 180 seconds
   ```
   **Solução**: Verificar logs do container para erros de configuração

### Validação Manual

```bash
# Verificar secrets no Azure Key Vault
az keyvault secret list --vault-name vault-name

# Verificar container logs
docker logs auditoria-microservice

# Verificar health endpoints
curl http://localhost:8084/actuator/health
```

## 📝 Atualizações Futuras

Para adicionar novos segredos:

1. ➕ Adicionar ao Azure Key Vault
2. ➕ Atualizar script `Load Azure Key Vault secrets` no pipeline
3. ➕ Adicionar variável de ambiente no `docker run`
4. ➕ Atualizar esta documentação

---
**Última atualização**: 2 de setembro de 2025  
**Versão do Pipeline**: v1.0.0  
**Compatibilidade**: Java 24, Spring Boot 3.5.5
