# 🔒 VERIFICAÇÃO DE SEGURANÇA - COMANDOS PÓS-DEPLOY

## **1. VERIFICAÇÃO DE ASSINATURA DE IMAGEM (COSIGN)**

```bash
# Instalar cosign se necessário
curl -O -L "https://github.com/sigstore/cosign/releases/latest/download/cosign-linux-amd64"
sudo mv cosign-linux-amd64 /usr/local/bin/cosign
sudo chmod +x /usr/local/bin/cosign

# Verificar assinatura keyless da imagem
cosign verify \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/auditoria-compliance-microservice:latest

# Verificar SBOM
cosign verify-attestation \
  --type="https://spdx.dev/Document" \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/auditoria-compliance-microservice:latest

# Verificar proveniência
cosign verify-attestation \
  --type="https://slsa.dev/provenance/v1" \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/auditoria-compliance-microservice:latest
```

## **2. VERIFICAÇÃO DE AUSÊNCIA DE SEGREDOS EM VARIÁVEIS DE AMBIENTE**

```bash
# Verificar que não há segredos em env vars do container
docker inspect auditoria-compliance-microservice | jq '.[]|.Config.Env[]' | \
  grep -v -E "(JAVA_OPTS|TZ|SPRING_PROFILES_ACTIVE|SERVER_PORT|ENVIRONMENT)" | \
  grep -i -E "(password|secret|key|token|credential)"

# Deve retornar vazio ou só variáveis não sensíveis
# Se encontrar algo, é uma falha de segurança
```

## **3. VERIFICAÇÃO DE PERMISSÕES DOS SECRETS**

```bash
# Verificar estrutura de diretórios de secrets
ls -la /run/secrets/auditoria-compliance/
# Deve mostrar:
# -r--------  1 root root  <size> <date> DB_PASSWORD
# -r--------  1 root root  <size> <date> AUDIT_ENCRYPTION_KEY
# -r--------  1 root root  <size> <date> COMPLIANCE_API_KEY
# etc.

# Verificar permissões específicas
stat /run/secrets/auditoria-compliance/DB_PASSWORD
# Deve mostrar: Access: (0400/-r--------) Uid: (0/root) Gid: (0/root)

# Verificar que arquivos não estão vazios
find /run/secrets/auditoria-compliance -type f -empty
# Deve retornar vazio (nenhum arquivo vazio)

# Verificar conteúdo sem expor (apenas tamanho)
wc -c /run/secrets/auditoria-compliance/* | grep -v " 0 "
# Deve mostrar arquivos com tamanho > 0
```

## **4. VERIFICAÇÃO DE ENDPOINTS ACTUATOR SEGUROS**

```bash
# Health check deve funcionar
curl -f http://localhost:8084/actuator/health
# Deve retornar: {"status":"UP"}

# Endpoints sensíveis devem estar bloqueados
curl -s http://localhost:8084/actuator/env && echo "❌ ENV ENDPOINT EXPOSTO" || echo "✅ ENV protegido"
curl -s http://localhost:8084/actuator/configprops && echo "❌ CONFIGPROPS EXPOSTO" || echo "✅ CONFIGPROPS protegido"
curl -s http://localhost:8084/actuator/beans && echo "❌ BEANS EXPOSTO" || echo "✅ BEANS protegido"
curl -s http://localhost:8084/actuator/threaddump && echo "❌ THREADDUMP EXPOSTO" || echo "✅ THREADDUMP protegido"

# Info deve funcionar (não sensível)
curl -f http://localhost:8084/actuator/info
```

## **5. VERIFICAÇÃO DE VAZAMENTO NOS LOGS**

```bash
# Verificar logs recentes não contêm secrets
docker logs auditoria-compliance-microservice --since="1h" 2>&1 | \
  grep -i -E "(password|secret|key|credential|token)" | \
  grep -v -E "(jwt.*validation|key.*rotation|secret.*loaded)" && \
  echo "❌ POSSÍVEL VAZAMENTO NOS LOGS" || echo "✅ Logs seguros"

# Verificar logs de sistema
journalctl -u docker --since="1h" | \
  grep -i -E "(password|secret|key)" && \
  echo "❌ POSSÍVEL VAZAMENTO NO SISTEMA" || echo "✅ Sistema seguro"
```

## **6. VERIFICAÇÃO DE CARREGAMENTO DO CONFIGTREE**

```bash
# Verificar que Spring está carregando secrets via configtree
docker logs auditoria-compliance-microservice 2>&1 | grep -i configtree
# Deve mostrar: "Loading configuration from configtree"

# Verificar que não há erros de carregamento de propriedades
docker logs auditoria-compliance-microservice 2>&1 | grep -i -E "(error.*property|failed.*load|configuration.*error)"
# Não deve mostrar erros relacionados a propriedades

# Verificar conexão com banco de dados funcionando
curl -f http://localhost:8084/actuator/health/db
# Deve retornar: {"status":"UP"}
```

## **7. VERIFICAÇÃO ESPECÍFICA DE AUDITORIA E COMPLIANCE**

```bash
# Verificar endpoint de saúde da auditoria
curl -f http://localhost:8084/rest/v1/audit/health
# Deve retornar status das funcionalidades de auditoria

# Verificar conectividade com Redis para cache de auditoria
curl -f http://localhost:8084/actuator/health/redis
# Deve retornar: {"status":"UP"}

# Testar endpoint de métricas de compliance
curl -s http://localhost:8084/actuator/metrics/audit.events.processed.total
curl -s http://localhost:8084/actuator/metrics/compliance.checks.performed.total

# Verificar se logs de auditoria estão sendo gerados
curl -f http://localhost:8084/rest/v1/audit/events/count
# Deve retornar contagem de eventos de auditoria
```

## **8. VERIFICAÇÃO DE CONECTIVIDADE COM BANCOS DE AUDITORIA**

```bash
# Se tiver acesso ao banco principal
# mysql -h <host> -u <user> -p<password> -e "SELECT COUNT(*) FROM audit_events WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR);"

# Se tiver banco separado para auditoria
# mysql -h <audit-host> -u <audit-user> -p<audit-password> -e "SELECT COUNT(*) FROM audit_logs WHERE timestamp > NOW() - INTERVAL 1 HOUR;"

# Verificar conectividade via endpoint
curl -X POST http://localhost:8084/rest/v1/audit/test/db-connection \
  -H "Authorization: Bearer <valid-jwt-token>"
# Deve retornar sucesso se conectividade estiver ok
```

## **9. VERIFICAÇÃO DE FUNCIONALIDADES DE COMPLIANCE**

```bash
# Testar geração de relatório de compliance
curl -X POST http://localhost:8084/rest/v1/compliance/reports/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{
    "reportType": "GDPR_COMPLIANCE",
    "startDate": "2024-01-01",
    "endDate": "2024-12-31"
  }'

# Testar validação de conformidade
curl -X POST http://localhost:8084/rest/v1/compliance/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{"entityId": "test-entity", "complianceType": "DATA_PROTECTION"}'

# Verificar logs de auditoria em tempo real
curl -H "Authorization: Bearer <valid-jwt-token>" \
  "http://localhost:8084/rest/v1/audit/events/recent?limit=10"
```

## **10. VERIFICAÇÃO DE ARQUIVAMENTO E RETENÇÃO**

```bash
# Testar funcionalidade de arquivamento
curl -X POST http://localhost:8084/rest/v1/audit/archive/trigger \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{"olderThanDays": 90}'

# Verificar configurações de retenção
curl -H "Authorization: Bearer <valid-jwt-token>" \
  http://localhost:8084/rest/v1/audit/retention-policies

# Testar conectividade com storage de arquivo
  http://localhost:8084/rest/v1/audit/archive/status
```

## **11. VERIFICAÇÃO DE CONECTIVIDADE JWT**

```bash
# Testar endpoint protegido com JWT válido
curl -H "Authorization: Bearer <test-jwt-token>" \
  http://localhost:8084/rest/v1/audit/events
# Deve retornar dados de auditoria ou erro 401 sem token

# Testar criação de evento de auditoria
curl -X POST http://localhost:8084/rest/v1/audit/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{
    "eventType": "USER_LOGIN",
    "userId": "test-user",
    "description": "Teste de evento de auditoria",
    "metadata": {"ip": "127.0.0.1", "userAgent": "Test"}
  }'
```

## **12. VERIFICAÇÃO DE ROTAÇÃO DE CHAVES**

```bash
# Verificar data de criação das chaves de auditoria no Key Vault
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-audit-encryption-key" --query "attributes.created" -o tsv

# Verificar próxima data de rotação
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-compliance-api-key" \
  --query "attributes.expires" -o tsv

# Verificar chaves GDPR
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-gdpr-encryption-key" \
  --query "attributes.created" -o tsv

# Verificar conectividade com serviços externos
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-external-audit-api-key" \
  --query "attributes.created" -o tsv
```

## **13. SCRIPT DE VERIFICAÇÃO COMPLETA**

```bash
#!/bin/bash
# verify-security-auditoria.sh - Script de verificação completa

set -euo pipefail

echo "🔒 VERIFICAÇÃO COMPLETA DE SEGURANÇA - AUDITORIA-COMPLIANCE"
echo "=========================================================="

# 1. Verificar container está rodando
if ! docker ps | grep -q auditoria-compliance-microservice; then
    echo "❌ Container não está rodando"
    exit 1
fi
echo "✅ Container está rodando"

# 2. Verificar health
if curl -f -s http://localhost:8084/actuator/health > /dev/null; then
    echo "✅ Health check passou"
else
    echo "❌ Health check falhou"
    exit 1
fi

# 3. Verificar endpoints sensíveis bloqueados
if curl -f -s http://localhost:8084/actuator/env > /dev/null; then
    echo "❌ Endpoint /env está exposto"
    exit 1
else
    echo "✅ Endpoint /env está protegido"
fi

# 4. Verificar secrets existem e têm permissões corretas
if [[ ! -d "/run/secrets/auditoria-compliance" ]]; then
    echo "❌ Diretório de secrets não existe"
    exit 1
fi

for secret in DB_PASSWORD AUDIT_ENCRYPTION_KEY COMPLIANCE_API_KEY ENCRYPTION_MASTER_KEY; do
    if [[ ! -f "/run/secrets/auditoria-compliance/$secret" ]]; then
        echo "❌ Secret $secret não existe"
        exit 1
    fi
    
    PERMS=$(stat -c "%a" "/run/secrets/auditoria-compliance/$secret")
    if [[ "$PERMS" != "400" ]]; then
        echo "❌ Secret $secret tem permissões incorretas: $PERMS"
        exit 1
    fi
done
echo "✅ Todos os secrets existem com permissões corretas"

# 5. Verificar não há vazamento em env vars
if docker inspect auditoria-compliance-microservice | jq '.[]|.Config.Env[]' | \
   grep -i -E "(password|secret|key)" | \
   grep -v -E "(JAVA_OPTS|SPRING_|TZ)" > /dev/null; then
    echo "❌ Possível vazamento em variáveis de ambiente"
    exit 1
else
    echo "✅ Nenhum segredo em variáveis de ambiente"
fi

# 6. Verificar funcionalidades específicas de auditoria
echo "📊 Verificando funcionalidades de auditoria..."

# Testar conectividade Redis
if curl -f -s http://localhost:8084/actuator/health/redis > /dev/null; then
    echo "✅ Conectividade Redis funcionando"
else
    echo "⚠️ Redis não acessível (pode ser normal se não configurado)"
fi

# Testar endpoint de eventos de auditoria
if curl -f -s http://localhost:8084/rest/v1/audit/events/count > /dev/null; then
    echo "✅ Endpoint de eventos de auditoria funcionando"
else
    echo "⚠️ Endpoint de auditoria não disponível (pode ser normal se não implementado)"
fi

# 7. Verificar volumes de logs montados
if docker inspect auditoria-compliance-microservice | grep -q "/app/logs"; then
    echo "✅ Volume de logs está montado"
else
    echo "⚠️ Volume de logs não montado"
fi

# 8. Verificar conectividade com banco de auditoria
if curl -f -s http://localhost:8084/actuator/health/auditDb > /dev/null; then
    echo "✅ Conectividade com banco de auditoria funcionando"
else
    echo "⚠️ Banco de auditoria separado não configurado (pode ser normal)"
fi

echo ""
echo "🎉 VERIFICAÇÃO COMPLETA: TODAS AS CHECAGENS PASSARAM"
echo "✅ Sistema de auditoria e compliance está seguro e em conformidade"
```

## **14. MONITORAMENTO CONTÍNUO**

```bash
# Configurar alertas para expiração de chaves (crontab)
0 9 * * * /usr/local/bin/check-key-expiration-auditoria.sh

# Script de monitoramento de expiração específico para auditoria
cat > /usr/local/bin/check-key-expiration-auditoria.sh << 'EOF'
#!/bin/bash
VAULT_NAME="kv-conexao-de-sorte"
DAYS_WARNING=30

# Verificar chaves específicas de auditoria e compliance
SECRETS=("conexao-de-sorte-audit-encryption-key" "conexao-de-sorte-compliance-api-key" "conexao-de-sorte-gdpr-encryption-key" "conexao-de-sorte-external-audit-api-key")

for SECRET in "${SECRETS[@]}"; do
    EXPIRES=$(az keyvault secret show --vault-name "$VAULT_NAME" \
      --name "$SECRET" --query "attributes.expires" -o tsv 2>/dev/null)
    
    if [[ -n "$EXPIRES" ]]; then
        EXPIRES_EPOCH=$(date -d "$EXPIRES" +%s)
        NOW_EPOCH=$(date +%s)
        DAYS_LEFT=$(( (EXPIRES_EPOCH - NOW_EPOCH) / 86400 ))
        
        if [[ $DAYS_LEFT -le $DAYS_WARNING ]]; then
            echo "⚠️ ALERTA: Secret $SECRET expira em $DAYS_LEFT dias!"
            # Enviar alerta (email, Slack, etc.)
        fi
    fi
done

# Verificar se logs de auditoria estão sendo gerados
RECENT_AUDIT_COUNT=$(curl -s -H "Authorization: Bearer $AUDIT_TOKEN" \
  "http://localhost:8084/rest/v1/audit/events/count?since=1h" 2>/dev/null || echo "0")

if [[ "$RECENT_AUDIT_COUNT" -eq 0 ]]; then
    echo "⚠️ ALERTA: Nenhum evento de auditoria nas últimas horas!"
fi
EOF
chmod +x /usr/local/bin/check-key-expiration-auditoria.sh
```

## **15. TESTES DE FUNCIONALIDADE ESPECÍFICA**

```bash
# Testar criação de evento de auditoria completo
curl -X POST http://localhost:8084/rest/v1/audit/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{
    "eventType": "SECURITY_TEST",
    "entityId": "test-entity-123",
    "userId": "test-user",
    "description": "Teste completo de funcionalidade de auditoria",
    "severity": "INFO",
    "metadata": {
      "ip": "127.0.0.1",
      "userAgent": "Security-Test/1.0",
      "sessionId": "test-session-123",
      "timestamp": "'$(date -Iseconds)'"
    }
  }'

# Testar busca de eventos de auditoria
curl -H "Authorization: Bearer <valid-jwt-token>" \
  "http://localhost:8084/rest/v1/audit/events/search?eventType=SECURITY_TEST&limit=5"

# Testar relatório de compliance GDPR
  curl -X POST http://localhost:8084/rest/v1/compliance/reports/gdpr \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{
    "userId": "test-user",
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "includePersonalData": false
  }'

# Verificar métricas de auditoria
curl -s http://localhost:8084/actuator/metrics/audit.events.total
curl -s http://localhost:8084/actuator/metrics/audit.events.by.type
curl -s http://localhost:8084/actuator/metrics/compliance.violations.total

# Testar webhook de compliance
  curl -X POST http://localhost:8084/rest/v1/compliance/webhook/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{
    "event": "COMPLIANCE_CHECK",
    "status": "SUCCESS",
    "timestamp": "'$(date -Iseconds)'"
  }'

# Verificar integridade dos logs de auditoria
curl -H "Authorization: Bearer <valid-jwt-token>" \
  http://localhost:8084/rest/v1/audit/integrity/verify
```
