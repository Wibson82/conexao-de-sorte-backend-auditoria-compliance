package br.tec.facilitaservicos.auditoria.apresentacao.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para evento de auditoria
 */
@Schema(description = "Data Transfer Object for Audit Event")
public record EventoAuditoriaDto(
    @Schema(description = "Unique identifier of the audit event", example = "evt_123")
    @NotBlank String id,
    @Schema(description = "Type of the event", example = "USER_LOGIN")
    @NotBlank String tipoEvento,
    @Schema(description = "Timestamp of the event", example = "2025-01-01T12:00:00.000")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime timestamp,
    @Schema(description = "Identifier of the user who triggered the event", example = "usr_456")
    @NotBlank String usuarioId,
    @Schema(description = "Name of the user who triggered the event", example = "John Doe")
    String usuarioNome,
    @Schema(description = "Action performed", example = "User logged in successfully")
    @NotBlank String acaoRealizada,
    @Schema(description = "Type of the entity affected", example = "USER")
    String entidadeTipo,
    @Schema(description = "Identifier of the entity affected", example = "usr_456")
    String entidadeId,
    @Schema(description = "Name of the entity affected", example = "John Doe")
    String entidadeNome,
    @Schema(description = "Status of the event", example = "SUCCESS")
    String statusEvento,
    @Schema(description = "Severity of the event", example = "INFO")
    String severidade,
    @Schema(description = "IP address from where the event was triggered", example = "192.168.1.1")
    String ipOrigem,
    @Schema(description = "User agent of the client", example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ...")
    String userAgent,
    @Schema(description = "Additional metadata for the event")
    Map<String, Object> metadados,
    @Schema(description = "Hash of the event data for integrity check", example = "a1b2c3d4e5f6...")
    String hashEvento,
    @Schema(description = "Hash of the previous event in the audit trail", example = "f6e5d4c3b2a1...")
    String hashAnterior,
    @Schema(description = "Indicates if the event contains personal data", example = "true")
    Boolean dadosPessoais,
    @Schema(description = "Compliance category of the event", example = "LGPD")
    String categoriaCompliance,
    @Schema(description = "Date until which the event data should be retained", example = "2035-01-01T12:00:00.000")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime retencaoAte,
    @Schema(description = "Indicates if the event data has been anonymized", example = "false")
    Boolean anonimizado,
    @Schema(description = "Creation timestamp of the event record", example = "2025-01-01T12:00:00.000")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime createdAt
) {
}