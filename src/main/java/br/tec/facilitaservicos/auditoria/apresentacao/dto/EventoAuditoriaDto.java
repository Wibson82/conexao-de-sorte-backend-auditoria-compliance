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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime timestamp,
    @NotBlank String usuarioId,
    String usuarioNome,
    @NotBlank String acaoRealizada,
    String entidadeTipo,
    String entidadeId,
    String entidadeNome,
    String statusEvento,
    String severidade,
    String ipOrigem,
    String userAgent,
    Map<String, Object> metadados,
    String hashEvento,
    String hashAnterior,
    Boolean dadosPessoais,
    String categoriaCompliance,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime retencaoAte,
    Boolean anonimizado,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime createdAt
) {
}