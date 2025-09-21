# 🔐 Mapa de Uso de Segredos - Auditoria & Compliance

## 📋 Resumo Executivo

Este documento mapeia **exatamente** quais segredos são necessários por cada job/serviço, seguindo o **princípio do mínimo** conforme especificado no prompt de auditoria.

---

## 🎯 Segredos por Job/Contexto

### **Job: build_and_deploy_backend**

#### **Segredos Mínimos Necessários:**

```yaml
# Azure Key Vault - Lista Explícita (SEM curingas)
secrets: |
  conexao-de-sorte-database-r2dbc-url
  conexao-de-sorte-database-username
  conexao-de-sorte-database-password
  conexao-de-sorte-redis-host
  conexao-de-sorte-redis-port
  conexao-de-sorte-redis-password
  conexao-de-sorte-redis-database
  conexao-de-sorte-jwt-issuer
  conexao-de-sorte-jwt-jwks-uri
  conexao-de-sorte-jwt-secret
  conexao-de-sorte-jwt-signing-key
  conexao-de-sorte-jwt-verification-key
```

#### **Justificativa por Segredo:**

| Segredo | Uso | Arquivo de Configuração |
|---------|-----|------------------------|
| `conexao-de-sorte-database-r2dbc-url` | Conexão R2DBC com MySQL | `application-azure.yml` |
| `conexao-de-sorte-database-username` | Usuário do banco | `application-azure.yml` |
| `conexao-de-sorte-database-password` | Senha do banco | `application-azure.yml` |
| `conexao-de-sorte-redis-host` | Host do Redis | `application-azure.yml` |
| `conexao-de-sorte-redis-port` | Porta do Redis | `application-azure.yml` |
| `conexao-de-sorte-redis-password` | Senha do Redis | `application-azure.yml` |
| `conexao-de-sorte-redis-database` | Database do Redis | `application-azure.yml` |
| `conexao-de-sorte-jwt-issuer` | Emissor JWT para validação | `application-azure.yml` |
| `conexao-de-sorte-jwt-jwks-uri` | URI JWKS para validação | `application-azure.yml` |
| `conexao-de-sorte-jwt-secret` | Chave secreta JWT | `application-azure.yml` |
| `conexao-de-sorte-jwt-signing-key` | Chave de assinatura JWT | `application-azure.yml` |
| `conexao-de-sorte-jwt-verification-key` | Chave de verificação JWT | `application-azure.yml` |

---

## 🚫 Segredos NÃO Necessários

### **Removidos da Lista Original:**

- ❌ `conexao-de-sorte-jwt-privateKey` - Não usado neste microserviço
- ❌ `conexao-de-sorte-jwt-publicKey` - Não usado neste microserviço
- ❌ `conexao-de-sorte-jwt-key-id` - Não usado neste microserviço
- ❌ `conexao-de-sorte-database-jdbc-url` - Usa apenas R2DBC
- ❌ `conexao-de-sorte-database-url` - Duplicado/não usado
- ❌ Todos os segredos de criptografia - Não implementados neste microserviço
- ❌ Todos os segredos CORS - Configurados via properties, não secrets

---

## 🔒 Política de Mascaramento

### **Segredos que DEVEM ser mascarados:**

```bash
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-database-password }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-redis-password }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-jwt-secret }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-jwt-signing-key }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-jwt-verification-key }}"
```

---

## 📊 Estatísticas

- **Total de segredos necessários:** 12
- **Segredos de banco:** 3
- **Segredos de Redis:** 4
- **Segredos JWT:** 5
- **Segredos removidos da lista original:** 15+

---

## ✅ Validação

Este mapeamento foi validado contra:

1. ✅ `application-azure.yml` - Configurações Spring Boot
2. ✅ `docker-compose.yml` - Variáveis de ambiente
3. ✅ `Dockerfile` - Build args necessários
4. ✅ Código fonte Java - Uso efetivo dos segredos

**Data da última validação:** $(date '+%Y-%m-%d %H:%M:%S')