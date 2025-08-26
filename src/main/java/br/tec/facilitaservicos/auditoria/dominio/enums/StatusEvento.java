package br.tec.facilitaservicos.auditoria.dominio.enums;

/**
 * ============================================================================
 * 📋 STATUS DO EVENTO DE AUDITORIA
 * ============================================================================
 * 
 * Enum que define os estados do ciclo de vida de um evento de auditoria:
 * 
 * Fluxo de Estados:
 * CRIADO → VALIDADO → PROCESSADO → ARQUIVADO
 *    ↓         ↓           ↓          ↓
 * REJEITADO  FALHA    REPROCESSO   EXPIRADO
 * 
 * Estados finais: ARQUIVADO, REJEITADO, EXPIRADO, ANONIMIZADO
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum StatusEvento {
    
    /**
     * Evento criado mas não validado
     */
    CRIADO("criado", "Evento criado", false),
    
    /**
     * Evento validado e íntegro
     */
    VALIDADO("validado", "Evento validado", false),
    
    /**
     * Evento processado e indexado
     */
    PROCESSADO("processado", "Evento processado", false),
    
    /**
     * Evento arquivado para longo prazo
     */
    ARQUIVADO("arquivado", "Evento arquivado", true),
    
    /**
     * Evento rejeitado por falha de validação
     */
    REJEITADO("rejeitado", "Evento rejeitado", true),
    
    /**
     * Falha no processamento do evento
     */
    FALHA("falha", "Falha no processamento", false),
    
    /**
     * Evento em reprocessamento
     */
    REPROCESSO("reprocesso", "Em reprocessamento", false),
    
    /**
     * Evento expirado pela política de retenção
     */
    EXPIRADO("expirado", "Evento expirado", true),
    
    /**
     * Dados anonimizados (LGPD/GDPR)
     */
    ANONIMIZADO("anonimizado", "Dados anonimizados", true);

    private final String codigo;
    private final String descricao;
    private final boolean estadoFinal;

    StatusEvento(String codigo, String descricao, boolean estadoFinal) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.estadoFinal = estadoFinal;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isEstadoFinal() {
        return estadoFinal;
    }

    /**
     * Busca status por código
     */
    public static StatusEvento fromCodigo(String codigo) {
        for (StatusEvento status : values()) {
            if (status.getCodigo().equals(codigo)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status não encontrado: " + codigo);
    }

    /**
     * Verifica se pode transicionar para outro status
     */
    public boolean podeTransicionarPara(StatusEvento novoStatus) {
        if (this.isEstadoFinal()) {
            return false; // Estados finais não podem mudar
        }

        return switch (this) {
            case CRIADO -> novoStatus == VALIDADO || novoStatus == REJEITADO;
            case VALIDADO -> novoStatus == PROCESSADO || novoStatus == FALHA;
            case PROCESSADO -> novoStatus == ARQUIVADO || novoStatus == ANONIMIZADO;
            case FALHA -> novoStatus == REPROCESSO || novoStatus == REJEITADO;
            case REPROCESSO -> novoStatus == PROCESSADO || novoStatus == REJEITADO;
            default -> false;
        };
    }

    /**
     * Próximo status natural na progressão
     */
    public StatusEvento proximoStatus() {
        return switch (this) {
            case CRIADO -> VALIDADO;
            case VALIDADO -> PROCESSADO;
            case PROCESSADO -> ARQUIVADO;
            case FALHA -> REPROCESSO;
            case REPROCESSO -> PROCESSADO;
            default -> this; // Estados finais não mudam
        };
    }

    /**
     * Verifica se evento precisa de ação
     */
    public boolean precisaAcao() {
        return this == CRIADO || this == FALHA || this == REPROCESSO;
    }

    /**
     * Cor para dashboard (UI)
     */
    public String getCor() {
        return switch (this) {
            case PROCESSADO, ARQUIVADO -> "green";
            case VALIDADO -> "blue";
            case CRIADO, REPROCESSO -> "yellow";
            case FALHA, REJEITADO -> "red";
            case EXPIRADO, ANONIMIZADO -> "gray";
        };
    }
}