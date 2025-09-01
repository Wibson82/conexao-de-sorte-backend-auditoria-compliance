#!/usr/bin/env bash
set -euo pipefail

# Validates access symptoms for Kafka/PostgreSQL and basic audit endpoints
# Usage: BASE_URL=http://localhost:8085 ./scripts/validate-kafka-postgres-events.sh

BASE_URL="${BASE_URL:-http://localhost:8085}"

echo "[auditoria] Checking health/metrics"
curl -fsS "$BASE_URL/actuator/health" >/dev/null && echo "[auditoria] ✅ health"
curl -fsS "$BASE_URL/actuator/metrics" >/dev/null && echo "[auditoria] ✅ metrics"

echo "[auditoria] Checking endpoints (expect 2xx/3xx/401/403)"
for p in "/rest/v1/audit/events" "/rest/v1/audit/reports"; do
  code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL$p")
  echo "[auditoria] $p -> HTTP $code"
done

echo "[auditoria] Kafka/PostgreSQL validation requires environment tooling (kafka-topics.sh/psql)." 
echo "[auditoria] TIP: run 'kafka-topics.sh --list --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS' and 'psql $JDBC_URL -c "\l"' in staging."

echo "[auditoria] Done"

