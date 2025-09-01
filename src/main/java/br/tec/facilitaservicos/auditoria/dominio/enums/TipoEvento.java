package br.tec.facilitaservicos.auditoria.dominio.enums;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "Login realizado com sucesso")
    LOGIN_SUCESSO("auth.login.success", "Login realizado com sucesso", false),
    @Schema(description = "Tentativa de login falhada")
    LOGIN_FALHA("auth.login.failure", "Tentativa de login falhada", true),
    @Schema(description = "Login bloqueado por tentativas")
    LOGIN_BLOQUEADO("auth.login.blocked", "Login bloqueado por tentativas", true),
    @Schema(description = "Logout realizado")
    LOGOUT("auth.logout", "Logout realizado", false),
    @Schema(description = "Senha alterada pelo usuário")
    SENHA_ALTERADA("auth.password.changed", "Senha alterada pelo usuário", true),
    @Schema(description = "Reset de senha solicitado")
    SENHA_RESET("auth.password.reset", "Reset de senha solicitado", true),
    @Schema(description = "Token JWT criado")
    TOKEN_CRIADO("auth.token.created", "Token JWT criado", false),
    @Schema(description = "Token JWT renovado")
    TOKEN_RENOVADO("auth.token.refreshed", "Token JWT renovado", false),
    @Schema(description = "Token JWT revogado")
    TOKEN_REVOGADO("auth.token.revoked", "Token JWT revogado", true),
    
    // === AUTORIZAÇÃO ===
    @Schema(description = "Acesso negado a recurso")
    ACESSO_NEGADO("auth.access.denied", "Acesso negado a recurso", true),
    @Schema(description = "Permissão concedida")
    PERMISSAO_CONCEDIDA("auth.permission.granted", "Permissão concedida", true),
    @Schema(description = "Permissão revogada")
    PERMISSAO_REVOGADA("auth.permission.revoked", "Permissão revogada", true),
    @Schema(description = "Role atribuído ao usuário")
    ROLE_ATRIBUIDO("auth.role.assigned", "Role atribuído ao usuário", true),
    @Schema(description = "Role removido do usuário")
    ROLE_REMOVIDO("auth.role.removed", "Role removido do usuário", true),
    
    // === DADOS PESSOAIS (LGPD) ===
    @Schema(description = "Dados pessoais criados")
    DADOS_CRIADOS("data.created", "Dados pessoais criados", true),
    @Schema(description = "Dados pessoais acessados")
    DADOS_ACESSADOS("data.accessed", "Dados pessoais acessados", true),
    @Schema(description = "Dados pessoais modificados")
    DADOS_MODIFICADOS("data.modified", "Dados pessoais modificados", true),
    @Schema(description = "Dados pessoais excluídos")
    DADOS_EXCLUIDOS("data.deleted", "Dados pessoais excluídos", true),
    @Schema(description = "Dados pessoais exportados")
    DADOS_EXPORTADOS("data.exported", "Dados pessoais exportados", true),
    @Schema(description = "Dados pessoais anonimizados")
    DADOS_ANONIMIZADOS("data.anonymized", "Dados pessoais anonimizados", true),
    
    // === OPERAÇÕES DE SISTEMA ===
    @Schema(description = "Usuário criado no sistema")
    USUARIO_CRIADO("user.created", "Usuário criado no sistema", true),
    @Schema(description = "Dados do usuário atualizados")
    USUARIO_ATUALIZADO("user.updated", "Dados do usuário atualizados", true),
    @Schema(description = "Usuário desativado")
    USUARIO_DESATIVADO("user.deactivated", "Usuário desativado", true),
    @Schema(description = "Usuário reativado")
    USUARIO_REATIVADO("user.reactivated", "Usuário reativado", true),
    
    // === FINANCEIRO ===
    @Schema(description = "Transação financeira criada")
    TRANSACAO_CRIADA("finance.transaction.created", "Transação financeira criada", true),
    @Schema(description = "Transação aprovada")
    TRANSACAO_APROVADA("finance.transaction.approved", "Transação aprovada", true),
    @Schema(description = "Transação rejeitada")
    TRANSACAO_REJEITADA("finance.transaction.rejected", "Transação rejeitada", true),
    @Schema(description = "Pagamento processado")
    PAGAMENTO_PROCESSADO("finance.payment.processed", "Pagamento processado", true),
    @Schema(description = "Saldo da conta alterado")
    SALDO_ALTERADO("finance.balance.changed", "Saldo da conta alterado", true),
    
    // === COMUNICAÇÃO ===
    @Schema(description = "Mensagem enviada")
    MENSAGEM_ENVIADA("comm.message.sent", "Mensagem enviada", false),
    @Schema(description = "Mensagem editada")
    MENSAGEM_EDITADA("comm.message.edited", "Mensagem editada", false),
    @Schema(description = "Mensagem excluída")
    MENSAGEM_EXCLUIDA("comm.message.deleted", "Mensagem excluída", false),
    @Schema(description = "Notificação enviada")
    NOTIFICACAO_ENVIADA("comm.notification.sent", "Notificação enviada", false),
    @Schema(description = "Email enviado")
    EMAIL_ENVIADO("comm.email.sent", "Email enviado", false),
    
    // === CONFIGURAÇÕES ===
    @Schema(description = "Configuração do sistema alterada")
    CONFIG_ALTERADA("system.config.changed", "Configuração do sistema alterada", true),
    @Schema(description = "Feature flag alterada")
    FEATURE_FLAG_ALTERADA("system.feature.toggled", "Feature flag alterada", true),
    @Schema(description = "Cache limpo")
    CACHE_LIMPO("system.cache.cleared", "Cache limpo", false),
    @Schema(description = "Backup realizado")
    BACKUP_REALIZADO("system.backup.completed", "Backup realizado", false),
    
    // === SEGURANÇA ===
    @Schema(description = "Tentativa de intrusão detectada")
    TENTATIVA_INTRUSION("security.intrusion.attempt", "Tentativa de intrusão detectada", true),
    @Schema(description = "Chave criptográfica rotacionada")
    CHAVE_ROTACIONADA("security.key.rotated", "Chave criptográfica rotacionada", true),
    @Schema(description = "Certificado renovado")
    CERTIFICADO_RENOVADO("security.certificate.renewed", "Certificado renovado", false),
    
    // === COMPLIANCE ===
    @Schema(description = "Consentimento LGPD dado")
    CONSENTIMENTO_DADO("compliance.consent.given", "Consentimento LGPD dado", true),
    @Schema(description = "Consentimento LGPD retirado")
    CONSENTIMENTO_RETIRADO("compliance.consent.withdrawn", "Consentimento LGPD retirado", true),
    @Schema(description = "Direito ao esquecimento exercido")
    DIREITO_ESQUECIMENTO("compliance.right.forgotten", "Direito ao esquecimento exercido", true),
    @Schema(description = "Portabilidade de dados solicitada")
    PORTABILIDADE_DADOS("compliance.data.portability", "Portabilidade de dados solicitada", true),
    
    // === ERROS E EXCEÇÕES ===
    @Schema(description = "Erro de aplicação")
    ERRO_APLICACAO("error.application", "Erro de aplicação", true),
    @Schema(description = "Erro de banco de dados")
    ERRO_BANCO_DADOS("error.database", "Erro de banco de dados", true),
    @Schema(description = "Erro de integração externa")
    ERRO_INTEGRACAO("error.integration", "Erro de integração externa", true),
    @Schema(description = "Erro de autenticação")
    ERRO_AUTENTICACAO("error.authentication", "Erro de autenticação", true),
    
    // === MONITORAMENTO ===
    @Schema(description = "Limite de rate excedido")
    LIMITE_RATE_EXCEDIDO("monitor.rate.limit.exceeded", "Limite de rate excedido", true),
    @Schema(description = "Recurso indisponível")
    RECURSO_INDISPONIVEL("monitor.resource.unavailable", "Recurso indisponível", true),
    @Schema(description = "Alerta de monitoramento disparado")
    ALERTA_DISPARADO("monitor.alert.triggered", "Alerta de monitoramento disparado", true),
    
    // === OUTROS ===
    @Schema(description = "Evento customizado")
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