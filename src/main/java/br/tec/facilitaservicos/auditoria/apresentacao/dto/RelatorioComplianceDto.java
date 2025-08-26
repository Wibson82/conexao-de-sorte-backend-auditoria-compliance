package br.tec.facilitaservicos.auditoria.apresentacao.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * DTO para relatório de compliance
 */
public record RelatorioComplianceDto(
    String id,
    String tipoRelatorio,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime periodoInicio,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime periodoFim,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime geradoEm,
    String geradoPor,
    ResumoExecutivo resumoExecutivo,
    List<ViolacaoCompliance> violacoes,
    List<RecomendacaoCompliance> recomendacoes,
    Map<String, Object> metricas,
    Map<String, Object> anexos
) {
    
    public record ResumoExecutivo(
        int totalEventos,
        int eventosComViolacao,
        double percentualCompliance,
        int dadosPessoaisAcessados,
        int solicitacoesEsquecimento,
        int exportacoesDados,
        StatusCompliance statusGeral
    ) {}
    
    public record ViolacaoCompliance(
        String id,
        TipoViolacao tipo,
        String descricao,
        SeveridadeViolacao severidade,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime detectadaEm,
        String entidadeAfetada,
        String dadosEnvolvidos,
        StatusViolacao status,
        List<String> acaosTomadas
    ) {}
    
    public record RecomendacaoCompliance(
        String categoria,
        String titulo,
        String descricao,
        PrioridadeRecomendacao prioridade,
        String impactoEstimado,
        List<String> passos
    ) {}
    
    public enum StatusCompliance {
        CONFORME,
        NAO_CONFORME,
        PARCIALMENTE_CONFORME,
        REQUER_ATENCAO
    }
    
    public enum TipoViolacao {
        ACESSO_NAO_AUTORIZADO,
        RETENCAO_EXCESSIVA,
        FALTA_CONSENTIMENTO,
        TRANSFERENCIA_INADEQUADA,
        FALTA_CRIPTOGRAFIA,
        AUSENCIA_REGISTRO
    }
    
    public enum SeveridadeViolacao {
        CRITICA,
        ALTA,
        MEDIA,
        BAIXA
    }
    
    public enum StatusViolacao {
        ABERTA,
        EM_REMEDACAO,
        RESOLVIDA,
        ACEITA_COMO_RISCO
    }
    
    public enum PrioridadeRecomendacao {
        URGENTE,
        ALTA,
        MEDIA,
        BAIXA
    }
}