#!/usr/bin/env bash
set -euo pipefail

# Verify staging readiness for Audit/Compliance service
# Usage: BASE_URL=http://localhost:8085 ./scripts/verify-staging.sh

BASE_URL="${BASE_URL:-http://localhost:8085}"
echo "[auditoria] Verificando em: $BASE_URL"

curl -fsS "$BASE_URL/actuator/health" >/dev/null && echo "[auditoria] ✅ actuator/health"
curl -fsS "$BASE_URL/actuator/metrics" >/dev/null && echo "[auditoria] ✅ actuator/metrics"

# Endpoint de eventos (deve exigir auth; aceitar 401/403)
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/rest/v1/audit/events")
echo "[auditoria] events -> HTTP $code"

echo "[auditoria] ✅ Verificações básicas concluídas"

