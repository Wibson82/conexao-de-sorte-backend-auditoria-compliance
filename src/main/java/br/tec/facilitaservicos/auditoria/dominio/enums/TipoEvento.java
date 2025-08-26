package br.tec.facilitaservicos.auditoria.dominio.enums;

/**
 * ============================================================================
 * 📋 TIPOS DE EVENTO DE AUDITORIA
 * ============================================================================
 * 
 * Enum que define todos os tipos de eventos auditáveis no sistema:
 * 
 * Categorias principais:
 * - AUTENTICACAO: Login, logout, mudanças de senha
 * - AUTORIZACAO: Permissões, roles, acessos negados
 * - DADOS: CRUD operations, mudanças de estado
 * - SISTEMA: Configurações, manutenção, erros
 * - COMPLIANCE: LGPD, GDPR, políticas de privacidade
 * - FINANCEIRO: Transações, pagamentos, cobranças
 * - COMUNICACAO: Mensagens, notificações, chat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum TipoEvento {
    
    // === AUTENTICAÇÃO ===
    LOGIN_SUCESSO("auth.login.success", "Login realizado com sucesso", false),
    LOGIN_FALHA("auth.login.failure", "Tentativa de login falhada", true),
    LOGIN_BLOQUEADO("auth.login.blocked", "Login bloqueado por tentativas", true),
    LOGOUT("auth.logout", "Logout realizado", false),
    SENHA_ALTERADA("auth.password.changed", "Senha alterada pelo usuário", true),
    SENHA_RESET("auth.password.reset", "Reset de senha solicitado", true),
    TOKEN_CRIADO("auth.token.created", "Token JWT criado", false),
    TOKEN_RENOVADO("auth.token.refreshed", "Token JWT renovado", false),
    TOKEN_REVOGADO("auth.token.revoked", "Token JWT revogado", true),
    
    // === AUTORIZAÇÃO ===
    ACESSO_NEGADO("auth.access.denied", "Acesso negado a recurso", true),
    PERMISSAO_CONCEDIDA("auth.permission.granted", "Permissão concedida", true),
    PERMISSAO_REVOGADA("auth.permission.revoked", "Permissão revogada", true),
    ROLE_ATRIBUIDO("auth.role.assigned", "Role atribuído ao usuário", true),
    ROLE_REMOVIDO("auth.role.removed", "Role removido do usuário", true),
    
    // === DADOS PESSOAIS (LGPD) ===
    DADOS_CRIADOS("data.created", "Dados pessoais criados", true),
    DADOS_ACESSADOS("data.accessed", "Dados pessoais acessados", true),
    DADOS_MODIFICADOS("data.modified", "Dados pessoais modificados", true),
    DADOS_EXCLUIDOS("data.deleted", "Dados pessoais excluídos", true),
    DADOS_EXPORTADOS("data.exported", "Dados pessoais exportados", true),
    DADOS_ANONIMIZADOS("data.anonymized", "Dados pessoais anonimizados", true),
    
    // === OPERAÇÕES DE SISTEMA ===
    USUARIO_CRIADO("user.created", "Usuário criado no sistema", true),
    USUARIO_ATUALIZADO("user.updated", "Dados do usuário atualizados", true),
    USUARIO_DESATIVADO("user.deactivated", "Usuário desativado", true),
    USUARIO_REATIVADO("user.reactivated", "Usuário reativado", true),
    
    // === FINANCEIRO ===
    TRANSACAO_CRIADA("finance.transaction.created", "Transação financeira criada", true),
    TRANSACAO_APROVADA("finance.transaction.approved", "Transação aprovada", true),
    TRANSACAO_REJEITADA("finance.transaction.rejected", "Transação rejeitada", true),
    PAGAMENTO_PROCESSADO("finance.payment.processed", "Pagamento processado", true),
    SALDO_ALTERADO("finance.balance.changed", "Saldo da conta alterado", true),
    
    // === COMUNICAÇÃO ===
    MENSAGEM_ENVIADA("comm.message.sent", "Mensagem enviada", false),
    MENSAGEM_EDITADA("comm.message.edited", "Mensagem editada", false),
    MENSAGEM_EXCLUIDA("comm.message.deleted", "Mensagem excluída", false),
    NOTIFICACAO_ENVIADA("comm.notification.sent", "Notificação enviada", false),
    EMAIL_ENVIADO("comm.email.sent", "Email enviado", false),
    
    // === CONFIGURAÇÕES ===
    CONFIG_ALTERADA("system.config.changed", "Configuração do sistema alterada", true),
    FEATURE_FLAG_ALTERADA("system.feature.toggled", "Feature flag alterada", true),
    CACHE_LIMPO("system.cache.cleared", "Cache limpo", false),
    BACKUP_REALIZADO("system.backup.completed", "Backup realizado", false),
    
    // === SEGURANÇA ===
    TENTATIVA_INTRUSION("security.intrusion.attempt", "Tentativa de intrusão detectada", true),
    CHAVE_ROTACIONADA("security.key.rotated", "Chave criptográfica rotacionada", true),
    CERTIFICADO_RENOVADO("security.certificate.renewed", "Certificado renovado", false),
    
    // === COMPLIANCE ===
    CONSENTIMENTO_DADO("compliance.consent.given", "Consentimento LGPD dado", true),
    CONSENTIMENTO_RETIRADO("compliance.consent.withdrawn", "Consentimento LGPD retirado", true),
    DIREITO_ESQUECIMENTO("compliance.right.forgotten", "Direito ao esquecimento exercido", true),
    PORTABILIDADE_DADOS("compliance.data.portability", "Portabilidade de dados solicitada", true),
    
    // === ERROS E EXCEÇÕES ===
    ERRO_APLICACAO("error.application", "Erro de aplicação", true),
    ERRO_BANCO_DADOS("error.database", "Erro de banco de dados", true),
    ERRO_INTEGRACAO("error.integration", "Erro de integração externa", true),
    ERRO_AUTENTICACAO("error.authentication", "Erro de autenticação", true),
    
    // === MONITORAMENTO ===
    LIMITE_RATE_EXCEDIDO("monitor.rate.limit.exceeded", "Limite de rate excedido", true),
    RECURSO_INDISPONIVEL("monitor.resource.unavailable", "Recurso indisponível", true),
    ALERTA_DISPARADO("monitor.alert.triggered", "Alerta de monitoramento disparado", true),
    
    // === OUTROS ===
    EVENTO_CUSTOMIZADO("custom.event", "Evento customizado", false);

    private final String codigo;
    private final String descricao;
    private final boolean critico;

    TipoEvento(String codigo, String descricao, boolean critico) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.critico = critico;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isCritico() {
        return critico;
    }

    /**
     * Busca tipo de evento por código
     */
    public static TipoEvento fromCodigo(String codigo) {
        for (TipoEvento tipo : values()) {
            if (tipo.getCodigo().equals(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de evento não encontrado: " + codigo);
    }

    /**
     * Verifica se evento requer dados pessoais
     */
    public boolean requerDadosPessoais() {
        return switch (this) {
            case DADOS_CRIADOS, DADOS_ACESSADOS, DADOS_MODIFICADOS, DADOS_EXCLUIDOS,
                 DADOS_EXPORTADOS, DADOS_ANONIMIZADOS,
                 USUARIO_CRIADO, USUARIO_ATUALIZADO,
                 CONSENTIMENTO_DADO, CONSENTIMENTO_RETIRADO,
                 DIREITO_ESQUECIMENTO, PORTABILIDADE_DADOS -> true;
            default -> false;
        };
    }

    /**
     * Categoria do evento para agrupamento
     */
    public String getCategoria() {
        return switch (this) {
            case LOGIN_SUCESSO, LOGIN_FALHA, LOGIN_BLOQUEADO, LOGOUT,
                 SENHA_ALTERADA, SENHA_RESET, TOKEN_CRIADO, TOKEN_RENOVADO, TOKEN_REVOGADO -> "AUTENTICACAO";
                 
            case ACESSO_NEGADO, PERMISSAO_CONCEDIDA, PERMISSAO_REVOGADA,
                 ROLE_ATRIBUIDO, ROLE_REMOVIDO -> "AUTORIZACAO";
                 
            case DADOS_CRIADOS, DADOS_ACESSADOS, DADOS_MODIFICADOS, DADOS_EXCLUIDOS,
                 DADOS_EXPORTADOS, DADOS_ANONIMIZADOS -> "DADOS_PESSOAIS";
                 
            case TRANSACAO_CRIADA, TRANSACAO_APROVADA, TRANSACAO_REJEITADA,
                 PAGAMENTO_PROCESSADO, SALDO_ALTERADO -> "FINANCEIRO";
                 
            case MENSAGEM_ENVIADA, MENSAGEM_EDITADA, MENSAGEM_EXCLUIDA,
                 NOTIFICACAO_ENVIADA, EMAIL_ENVIADO -> "COMUNICACAO";
                 
            case CONSENTIMENTO_DADO, CONSENTIMENTO_RETIRADO,
                 DIREITO_ESQUECIMENTO, PORTABILIDADE_DADOS -> "COMPLIANCE";
                 
            case TENTATIVA_INTRUSION, CHAVE_ROTACIONADA, CERTIFICADO_RENOVADO -> "SEGURANCA";
                 
            case ERRO_APLICACAO, ERRO_BANCO_DADOS, ERRO_INTEGRACAO, ERRO_AUTENTICACAO -> "ERROS";
                 
            default -> "SISTEMA";
        };
    }

    /**
     * Período de retenção em dias por tipo de evento
     */
    public int getPeriodoRetencaoDias() {
        return switch (this.getCategoria()) {
            case "COMPLIANCE", "DADOS_PESSOAIS" -> 2555; // 7 anos para LGPD
            case "FINANCEIRO" -> 1825; // 5 anos para transações
            case "AUTENTICACAO", "AUTORIZACAO" -> 1095; // 3 anos para security
            case "SEGURANCA" -> 2190; // 6 anos para incidentes
            case "ERROS" -> 365; // 1 ano para debug
            default -> 730; // 2 anos padrão
        };
    }
}