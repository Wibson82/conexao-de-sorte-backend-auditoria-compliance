package br.tec.facilitaservicos.auditoria.apresentacao.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para evento de auditoria
 */
public record EventoAuditoriaDto(
    @NotBlank String id,
    @NotBlank String tipoEvento,
    @NotBlank String entidade,
    @NotBlank String entidadeId,
    @NotBlank String usuarioId,
    String usuarioNome,
    String sessaoId,
    String correlationId,
    String traceId,
    Map<String, Object> dadosAntes,
    Map<String, Object> dadosDepois,
    Map<String, Object> metadados,
    @NotBlank String hash,
    String hashAnterior,
    String assinatura,
    @NotNull StatusIntegridade integridade,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime timestamp,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime criadoEm,
    String origem,
    String versaoEsquema,
    Map<String, Object> contextoSeguranca
) {
    
    public enum StatusIntegridade {
        VALIDA,
        CORROMPIDA,
        NAO_VERIFICADA,
        PENDENTE_VERIFICACAO
    }
}