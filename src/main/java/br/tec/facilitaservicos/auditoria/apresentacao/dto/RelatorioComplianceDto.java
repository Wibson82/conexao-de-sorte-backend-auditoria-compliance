package br.tec.facilitaservicos.auditoria.apresentacao.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para relatório de compliance
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Relatório detalhado de compliance e auditoria")
public record RelatorioComplianceDto(
    @Schema(description = "Identificador único do relatório", example = "rel-comp-123")
    String id,
    @Schema(description = "Tipo do relatório de compliance", example = "AUDITORIA_GERAL", required = true)
    @NotBlank(message = "Tipo do relatório é obrigatório")
    String tipoRelatorio,
    @Schema(description = "Data e hora de início do período do relatório", example = "2024-01-01T00:00:00.000")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime periodoInicio,
    @Schema(description = "Data e hora de fim do período do relatório", example = "2024-12-31T23:59:59.999")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime periodoFim,
    @Schema(description = "Data e hora de geração do relatório", example = "2025-01-01T10:00:00.000")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime geradoEm,
    @Schema(description = "Identificador do usuário ou sistema que gerou o relatório", example = "admin-user")
    String geradoPor,
    @Schema(description = "Resumo executivo do relatório de compliance")
    ResumoExecutivo resumoExecutivo,
    @Schema(description = "Lista de violações de compliance detectadas")
    List<ViolacaoCompliance> violacoes,
    @Schema(description = "Lista de recomendações para melhoria de compliance")
    List<RecomendacaoCompliance> recomendacoes,
    @Schema(description = "Métricas e estatísticas adicionais do relatório (JSON)", example = "{\"eventosPorDia\": 150, \"usuariosAfetados\": 20}")
    Map<String, Object> metricas,
    @Schema(description = "Informações sobre anexos ou documentos relacionados (JSON)", example = "{\"linkRelatorioCompleto\": \"http://example.com/report.pdf\"}")
    Map<String, Object> anexos
) {
    
    @Schema(description = "Resumo executivo das principais métricas de compliance")
    public record ResumoExecutivo(
        @Schema(description = "Total de eventos de auditoria no período", example = "10000")
        int totalEventos,
        @Schema(description = "Número de eventos com violação de compliance", example = "50")
        int eventosComViolacao,
        @Schema(description = "Percentual de conformidade (100 - percentual de violações)", example = "99.5")
        double percentualCompliance,
        @Schema(description = "Número de acessos a dados pessoais", example = "1200")
        int dadosPessoaisAcessados,
        @Schema(description = "Número de solicitações de direito ao esquecimento", example = "5")
        int solicitacoesEsquecimento,
        @Schema(description = "Número de exportações de dados pessoais", example = "10")
        int exportacoesDados,
        @Schema(description = "Status geral de conformidade")
        StatusCompliance statusGeral
    ) {}
    
    @Schema(description = "Detalhes de uma violação de compliance detectada")
    public record ViolacaoCompliance(
        @Schema(description = "Identificador único da violação", example = "viol-456")
        String id,
        @Schema(description = "Tipo da violação (ex: ACESSO_NAO_AUTORIZADO)")
        TipoViolacao tipo,
        @Schema(description = "Descrição detalhada da violação", example = "Tentativa de acesso a dados restritos por usuário não autorizado")
        String descricao,
        @Schema(description = "Severidade da violação")
        SeveridadeViolacao severidade,
        @Schema(description = "Data e hora em que a violação foi detectada", example = "2024-12-25T10:00:00.000")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime detectadaEm,
        @Schema(description = "Entidade afetada pela violação", example = "USUARIO")
        String entidadeAfetada,
        @Schema(description = "Dados ou recursos envolvidos na violação", example = "{\"userId\": \"user-789\", \"resource\": \"/api/admin/data\"}")
        String dadosEnvolvidos,
        @Schema(description = "Status atual da violação")
        StatusViolacao status,
        @Schema(description = "Ações tomadas para remediar a violação")
        List<String> acaosTomadas
    ) {}
    
    @Schema(description = "Recomendação para melhoria de compliance")
    public record RecomendacaoCompliance(
        @Schema(description = "Categoria da recomendação (ex: SEGURANCA, DADOS)")
        String categoria,
        @Schema(description = "Título da recomendação", example = "Implementar MFA para administradores")
        String titulo,
        @Schema(description = "Descrição detalhada da recomendação", example = "Exigir autenticação multifator para todos os usuários com privilégios administrativos")
        String descricao,
        @Schema(description = "Prioridade da recomendação")
        PrioridadeRecomendacao prioridade,
        @Schema(description = "Impacto estimado da implementação", example = "Redução de 80% no risco de acesso não autorizado")
        String impactoEstimado,
        @Schema(description = "Passos para implementar a recomendação")
        List<String> passos
    ) {}
    
    @Schema(description = "Status geral de conformidade do relatório")
    public enum StatusCompliance {
        @Schema(description = "Totalmente em conformidade")
        CONFORME,
        @Schema(description = "Não em conformidade")
        NAO_CONFORME,
        @Schema(description = "Parcialmente em conformidade")
        PARCIALMENTE_CONFORME,
        @Schema(description = "Requer atenção para conformidade")
        REQUER_ATENCAO
    }
    
    @Schema(description = "Tipo de violação de compliance")
    public enum TipoViolacao {
        @Schema(description = "Acesso a dados ou sistemas sem autorização")
        ACESSO_NAO_AUTORIZADO,
        @Schema(description = "Retenção de dados por período maior que o permitido")
        RETENCAO_EXCESSIVA,
        @Schema(description = "Processamento de dados sem o consentimento necessário")
        FALTA_CONSENTIMENTO,
        @Schema(description = "Transferência de dados para locais ou entidades não autorizadas")
        TRANSFERENCIA_INADEQUADA,
        @Schema(description = "Dados sensíveis não criptografados ou com criptografia inadequada")
        FALTA_CRIPTOGRAFIA,
        @Schema(description = "Ausência de registro de atividades obrigatórias")
        AUSENCIA_REGISTRO
    }
    
    @Schema(description = "Nível de severidade da violação")
    public enum SeveridadeViolacao {
        @Schema(description = "Violação crítica, exige ação imediata")
        CRITICA,
        @Schema(description = "Violação de alta severidade")
        ALTA,
        @Schema(description = "Violação de severidade média")
        MEDIA,
        @Schema(description = "Violação de baixa severidade")
        BAIXA
    }
    
    @Schema(description = "Status atual da violação de compliance")
    public enum StatusViolacao {
        @Schema(description = "Violação detectada e aguardando tratamento")
        ABERTA,
        @Schema(description = "Violação em processo de remediação")
        EM_REMEDACAO,
        @Schema(description = "Violação resolvida e remediada")
        RESOLVIDA,
        @Schema(description = "Violação aceita como risco de negócio")
        ACEITA_COMO_RISCO
    }
    
    @Schema(description = "Prioridade da recomendação de compliance")
    public enum PrioridadeRecomendacao {
        @Schema(description = "Recomendação urgente, exige ação imediata")
        URGENTE,
        @Schema(description = "Recomendação de alta prioridade")
        ALTA,
        @Schema(description = "Recomendação de prioridade média")
        MEDIA,
        @Schema(description = "Recomendação de baixa prioridade")
        BAIXA
    }
}