#!/bin/bash
# =============================================================================
# 🔐 SETUP SECRETS - CONFIGURAÇÃO SEGURA DE SEGREDOS NO SERVIDOR
# =============================================================================
# Script para configurar secrets do Azure Key Vault no servidor de produção
# usando o padrão /run/secrets com configtree do Spring Boot
# =============================================================================

set -euo pipefail

# ===== CONFIGURAÇÕES =====
SECRETS_DIR="/run/secrets"
SERVICE_USER="auditoria-user"
VAULT_NAME="kv-conexao-de-sorte"  # Placeholder - será configurado via env
LOG_FILE="/var/log/setup-secrets-auditoria.log"

# ===== FUNÇÕES =====
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] $1" | tee -a "$LOG_FILE"
}

error() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] $1" | tee -a "$LOG_FILE" >&2
    exit 1
}

create_secret_file() {
    local name=$1
    local value=$2
    local file="$SECRETS_DIR/$name"
    
    if [[ -z "$value" || "$value" == "null" ]]; then
        log "⚠️ Skipping $name (empty or null value)"
        return
    fi
    
    log "📝 Creating secret file: $name"
    
    # Create secret file with secure permissions
    echo "$value" | sudo tee "$file" > /dev/null
    
    # Set ownership and permissions
    sudo chown root:root "$file"
    sudo chmod 0400 "$file"
    
    # Allow app user to read via ACL (if available) or group
    if command -v setfacl >/dev/null 2>&1; then
        sudo setfacl -m u:$SERVICE_USER:r "$file" || {
            log "⚠️ ACL not available, using group permissions"
            sudo chgrp $SERVICE_USER "$file"
            sudo chmod 0440 "$file"
        }
    else
        sudo chgrp $SERVICE_USER "$file"
        sudo chmod 0440 "$file"
    fi
    
    log "✅ Secret $name created successfully"
}

# ===== MAIN EXECUTION =====
main() {
    log "🔐 Starting secure secrets setup for auditoria-compliance microservice..."
    
    # Create service user if not exists
    if ! id "$SERVICE_USER" >/dev/null 2>&1; then
        log "👤 Creating service user: $SERVICE_USER"
        sudo useradd -r -s /bin/false -M "$SERVICE_USER"
    fi
    
    # Create secrets directory
    log "📁 Setting up secrets directory: $SECRETS_DIR"
    sudo mkdir -p "$SECRETS_DIR"
    sudo chown root:root "$SECRETS_DIR"
    sudo chmod 755 "$SECRETS_DIR"
    
    # Authenticate with Azure using Managed Identity or Service Principal
    log "🔐 Authenticating with Azure..."
    if ! az account show >/dev/null 2>&1; then
        # Try managed identity first
        az login --identity || {
            error "❌ Azure authentication failed. Ensure managed identity or service principal is configured."
        }
    fi
    
    log "🔍 Fetching secrets from Azure Key Vault: $VAULT_NAME"
    
    # Fetch and create secret files
    # Database secrets
    log "🗄️ Processing database secrets..."
    DB_URL=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-url" --query value -o tsv 2>/dev/null || echo "")
    DB_USER=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-username" --query value -o tsv 2>/dev/null || echo "")
    DB_PASSWORD=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-password" --query value -o tsv 2>/dev/null || echo "")
    
    create_secret_file "DB_URL" "$DB_URL"
    create_secret_file "DB_USER" "$DB_USER"
    create_secret_file "DB_PASSWORD" "$DB_PASSWORD"
    
    # Audit-specific database secrets (read-only replicas for reports)
    log "📊 Processing audit database secrets..."
    AUDIT_DB_URL=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-audit-database-url" --query value -o tsv 2>/dev/null || echo "")
    AUDIT_DB_USER=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-audit-database-username" --query value -o tsv 2>/dev/null || echo "")
    AUDIT_DB_PASSWORD=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-audit-database-password" --query value -o tsv 2>/dev/null || echo "")
    
    create_secret_file "AUDIT_DB_URL" "$AUDIT_DB_URL"
    create_secret_file "AUDIT_DB_USER" "$AUDIT_DB_USER"
    create_secret_file "AUDIT_DB_PASSWORD" "$AUDIT_DB_PASSWORD"
    
    # Redis secrets for audit log caching
    log "🚀 Processing Redis secrets for audit caching..."
    REDIS_HOST=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-host" --query value -o tsv 2>/dev/null || echo "redis")
    REDIS_PORT=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-port" --query value -o tsv 2>/dev/null || echo "6379")
    REDIS_PASSWORD=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-password" --query value -o tsv 2>/dev/null || echo "")
    REDIS_DATABASE_AUDIT=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-database-audit" --query value -o tsv 2>/dev/null || echo "2")
    
    create_secret_file "REDIS_HOST" "$REDIS_HOST"
    create_secret_file "REDIS_PORT" "$REDIS_PORT"
    create_secret_file "REDIS_PASSWORD" "$REDIS_PASSWORD"
    create_secret_file "REDIS_DATABASE_AUDIT" "$REDIS_DATABASE_AUDIT"
    
    # JWT secrets (base64 decode for keys)
    log "🔑 Processing JWT secrets..."
    JWT_SIGNING_KEY_B64=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-jwt-signing-key" --query value -o tsv 2>/dev/null || echo "")
    JWT_VERIFICATION_KEY_B64=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-jwt-verification-key" --query value -o tsv 2>/dev/null || echo "")
    JWT_KEY_ID=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-jwt-key-id" --query value -o tsv 2>/dev/null || echo "")
    JWT_SECRET=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-jwt-secret" --query value -o tsv 2>/dev/null || echo "")
    
    # Decode base64 keys if they exist
    if [[ -n "$JWT_SIGNING_KEY_B64" && "$JWT_SIGNING_KEY_B64" != "null" ]]; then
        JWT_SIGNING_KEY=$(echo "$JWT_SIGNING_KEY_B64" | base64 -d)
        create_secret_file "JWT_SIGNING_KEY" "$JWT_SIGNING_KEY"
    fi
    
    if [[ -n "$JWT_VERIFICATION_KEY_B64" && "$JWT_VERIFICATION_KEY_B64" != "null" ]]; then
        JWT_VERIFICATION_KEY=$(echo "$JWT_VERIFICATION_KEY_B64" | base64 -d)
        create_secret_file "JWT_VERIFICATION_KEY" "$JWT_VERIFICATION_KEY"
    fi
    
    create_secret_file "JWT_KEY_ID" "$JWT_KEY_ID"
    create_secret_file "JWT_SECRET" "$JWT_SECRET"
    
    # Compliance and audit-specific secrets
    log "🛡️ Processing compliance and audit secrets..."
    AUDIT_ENCRYPTION_KEY=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-audit-encryption-key" --query value -o tsv 2>/dev/null || echo "")
    COMPLIANCE_API_KEY=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-compliance-api-key" --query value -o tsv 2>/dev/null || echo "")
    GDPR_ENCRYPTION_KEY=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-gdpr-encryption-key" --query value -o tsv 2>/dev/null || echo "")
    
    create_secret_file "AUDIT_ENCRYPTION_KEY" "$AUDIT_ENCRYPTION_KEY"
    create_secret_file "COMPLIANCE_API_KEY" "$COMPLIANCE_API_KEY"
    create_secret_file "GDPR_ENCRYPTION_KEY" "$GDPR_ENCRYPTION_KEY"
    
    # External compliance service secrets
    log "🔗 Processing external compliance service secrets..."
    EXTERNAL_AUDIT_API_URL=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-external-audit-api-url" --query value -o tsv 2>/dev/null || echo "")
    EXTERNAL_AUDIT_API_KEY=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-external-audit-api-key" --query value -o tsv 2>/dev/null || echo "")
    COMPLIANCE_WEBHOOK_SECRET=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-compliance-webhook-secret" --query value -o tsv 2>/dev/null || echo "")
    
    create_secret_file "EXTERNAL_AUDIT_API_URL" "$EXTERNAL_AUDIT_API_URL"
    create_secret_file "EXTERNAL_AUDIT_API_KEY" "$EXTERNAL_AUDIT_API_KEY"
    create_secret_file "COMPLIANCE_WEBHOOK_SECRET" "$COMPLIANCE_WEBHOOK_SECRET"
    
    # Data retention and archival secrets
    log "📚 Processing data retention secrets..."
    ARCHIVE_STORAGE_KEY=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-archive-storage-key" --query value -o tsv 2>/dev/null || echo "")
    ARCHIVE_STORAGE_CONNECTION=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-archive-storage-connection" --query value -o tsv 2>/dev/null || echo "")
    
    create_secret_file "ARCHIVE_STORAGE_KEY" "$ARCHIVE_STORAGE_KEY"
    create_secret_file "ARCHIVE_STORAGE_CONNECTION" "$ARCHIVE_STORAGE_CONNECTION"
    
    # Log forwarding secrets (for centralized logging)
    log "📋 Processing log forwarding secrets..."
    SIEM_API_KEY=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-siem-api-key" --query value -o tsv 2>/dev/null || echo "")
    SIEM_ENDPOINT=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-siem-endpoint" --query value -o tsv 2>/dev/null || echo "")
    LOG_AGGREGATOR_TOKEN=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-log-aggregator-token" --query value -o tsv 2>/dev/null || echo "")
    
    create_secret_file "SIEM_API_KEY" "$SIEM_API_KEY"
    create_secret_file "SIEM_ENDPOINT" "$SIEM_ENDPOINT"
    create_secret_file "LOG_AGGREGATOR_TOKEN" "$LOG_AGGREGATOR_TOKEN"
    
    # Encryption secrets
    log "🔐 Processing encryption secrets..."
    ENCRYPTION_MASTER_KEY=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-encryption-master-key" --query value -o tsv 2>/dev/null || echo "")
    create_secret_file "ENCRYPTION_MASTER_KEY" "$ENCRYPTION_MASTER_KEY"
    
    # Clear sensitive variables from memory
    unset DB_PASSWORD AUDIT_DB_PASSWORD REDIS_PASSWORD JWT_SECRET ENCRYPTION_MASTER_KEY
    unset JWT_SIGNING_KEY JWT_VERIFICATION_KEY JWT_SIGNING_KEY_B64 JWT_VERIFICATION_KEY_B64
    unset AUDIT_ENCRYPTION_KEY COMPLIANCE_API_KEY GDPR_ENCRYPTION_KEY
    unset EXTERNAL_AUDIT_API_KEY COMPLIANCE_WEBHOOK_SECRET ARCHIVE_STORAGE_KEY
    unset SIEM_API_KEY LOG_AGGREGATOR_TOKEN
    
    log "📋 Secrets setup verification:"
    ls -la "$SECRETS_DIR" | grep -v -E '^\s*total' || true
    
    log "✅ Secrets setup completed successfully!"
    log "📋 Next steps:"
    log "   1. Restart the auditoria-compliance microservice"
    log "   2. Verify configtree is loading secrets correctly"
    log "   3. Check application logs for any errors"
    log "   4. Test audit logging and compliance reporting functionality"
    log "   5. Verify external compliance service connectivity"
    log "   6. Validate data retention and archival processes"
    
    return 0
}

# ===== EXECUTION =====
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Check if running as root or with sudo
    if [[ $EUID -ne 0 ]]; then
        error "❌ This script must be run as root or with sudo"
    fi
    
    # Validate environment
    if ! command -v az >/dev/null 2>&1; then
        error "❌ Azure CLI not found. Please install it first."
    fi
    
    # Get vault name from environment or use default
    VAULT_NAME="${AZURE_KEYVAULT_NAME:-$VAULT_NAME}"
    
    log "🚀 Starting secrets setup with vault: $VAULT_NAME"
    main "$@"
fi