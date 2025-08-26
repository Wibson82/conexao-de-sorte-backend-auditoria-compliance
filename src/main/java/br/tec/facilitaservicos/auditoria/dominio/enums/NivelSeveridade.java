package br.tec.facilitaservicos.auditoria.dominio.enums;

/**
 * ============================================================================
 * 📋 NÍVEIS DE SEVERIDADE PARA AUDITORIA
 * ============================================================================
 * 
 * Enum que define os níveis de severidade dos eventos de auditoria
 * baseado em padrões de logging e monitoramento:
 * 
 * Hierarquia (do menor para maior):
 * DEBUG < INFO < WARN < ERROR < CRITICAL
 * 
 * Usado para:
 * - Priorização de alertas
 * - Filtragem de eventos
 * - Retenção diferenciada
 * - Notificações automáticas
 * - SLA de resposta
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum NivelSeveridade {
    
    /**
     * DEBUG - Informações técnicas detalhadas
     * Ex: Trace de execução, valores de variáveis
     */
    DEBUG(0, "debug", "Debug", false, false),
    
    /**
     * INFO - Informações gerais de operação
     * Ex: Login bem-sucedido, operação completada
     */
    INFO(1, "info", "Informativo", false, false),
    
    /**
     * WARN - Situações que requerem atenção
     * Ex: Tentativas de login falhadas, limites próximos
     */
    WARN(2, "warn", "Aviso", true, false),
    
    /**
     * ERROR - Erros que afetam operação
     * Ex: Falhas de integração, exceções não tratadas
     */
    ERROR(3, "error", "Erro", true, true),
    
    /**
     * CRITICAL - Situações críticas de segurança/sistema
     * Ex: Tentativas de intrusão, corrupção de dados
     */
    CRITICAL(4, "critical", "Crítico", true, true);

    private final int nivel;
    private final String codigo;
    private final String descricao;
    private final boolean requerNotificacao;
    private final boolean requerAcaoImediata;

    NivelSeveridade(int nivel, String codigo, String descricao, 
                    boolean requerNotificacao, boolean requerAcaoImediata) {
        this.nivel = nivel;
        this.codigo = codigo;
        this.descricao = descricao;
        this.requerNotificacao = requerNotificacao;
        this.requerAcaoImediata = requerAcaoImediata;
    }

    public int getNivel() {
        return nivel;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isRequerNotificacao() {
        return requerNotificacao;
    }

    public boolean isRequerAcaoImediata() {
        return requerAcaoImediata;
    }

    /**
     * Busca severidade por código
     */
    public static NivelSeveridade fromCodigo(String codigo) {
        for (NivelSeveridade severidade : values()) {
            if (severidade.getCodigo().equals(codigo)) {
                return severidade;
            }
        }
        throw new IllegalArgumentException("Nível de severidade não encontrado: " + codigo);
    }

    /**
     * Busca severidade por nível numérico
     */
    public static NivelSeveridade fromNivel(int nivel) {
        for (NivelSeveridade severidade : values()) {
            if (severidade.getNivel() == nivel) {
                return severidade;
            }
        }
        throw new IllegalArgumentException("Nível numérico não encontrado: " + nivel);
    }

    /**
     * Verifica se é mais severo que outro nível
     */
    public boolean isMaisSeveroQue(NivelSeveridade outro) {
        return this.nivel > outro.nivel;
    }

    /**
     * Verifica se é menos severo que outro nível
     */
    public boolean isMenosSeveroQue(NivelSeveridade outro) {
        return this.nivel < outro.nivel;
    }

    /**
     * Tempo de retenção baseado na severidade (em dias)
     */
    public int getPeriodoRetencaoBasico() {
        return switch (this) {
            case DEBUG -> 30;      // 1 mês
            case INFO -> 180;      // 6 meses
            case WARN -> 365;      // 1 ano
            case ERROR -> 1095;    // 3 anos
            case CRITICAL -> 2555; // 7 anos
        };
    }

    /**
     * Prioridade de processamento (1 = mais alta)
     */
    public int getPrioridadeProcessamento() {
        return switch (this) {
            case CRITICAL -> 1;
            case ERROR -> 2;
            case WARN -> 3;
            case INFO -> 4;
            case DEBUG -> 5;
        };
    }

    /**
     * Cor para dashboard (UI)
     */
    public String getCor() {
        return switch (this) {
            case DEBUG -> "purple";
            case INFO -> "blue";
            case WARN -> "orange";
            case ERROR -> "red";
            case CRITICAL -> "darkred";
        };
    }

    /**
     * Emoji para representação visual
     */
    public String getEmoji() {
        return switch (this) {
            case DEBUG -> "🔍";
            case INFO -> "ℹ️";
            case WARN -> "⚠️";
            case ERROR -> "❌";
            case CRITICAL -> "🚨";
        };
    }

    /**
     * Canal de notificação recomendado
     */
    public String getCanalNotificacao() {
        return switch (this) {
            case DEBUG, INFO -> "LOG";
            case WARN -> "EMAIL";
            case ERROR -> "SLACK";
            case CRITICAL -> "SMS";
        };
    }

    /**
     * SLA de resposta em minutos
     */
    public int getSlaRespostaMinutos() {
        return switch (this) {
            case DEBUG, INFO -> Integer.MAX_VALUE; // Sem SLA
            case WARN -> 240;      // 4 horas
            case ERROR -> 60;      // 1 hora
            case CRITICAL -> 15;   // 15 minutos
        };
    }
}