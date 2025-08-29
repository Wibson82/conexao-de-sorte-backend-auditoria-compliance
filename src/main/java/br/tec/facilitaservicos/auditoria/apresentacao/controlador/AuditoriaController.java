package br.tec.facilitaservicos.auditoria.apresentacao.controlador;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.tec.facilitaservicos.auditoria.aplicacao.servico.AuditoriaService;
import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import br.tec.facilitaservicos.auditoria.apresentacao.dto.RelatorioComplianceDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 📋 CONTROLLER REATIVO DE AUDITORIA
 * ============================================================================
 * 
 * Controller 100% reativo para auditoria e compliance:
 * - Consulta de trilhas de auditoria
 * - Verificação de integridade de eventos
 * - Relatórios de compliance
 * - Métricas de auditoria
 * - Validação de assinaturas digitais
 * 
 * Endpoints:
 * - GET /api/auditoria/eventos - Consultar eventos de auditoria
 * - GET /api/auditoria/trilha/{entidade}/{id} - Trilha de uma entidade
 * - GET /api/auditoria/integridade/{id} - Verificar integridade
 * - GET /api/auditoria/relatorio/compliance - Relatórios compliance
 * - POST /api/auditoria/verificar-assinatura - Verificar assinatura
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Auditoria", description = "API para consulta de trilhas de auditoria e compliance")
@SecurityRequirement(name = "bearerAuth")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    /**
     * Consulta eventos de auditoria com filtros
     */
    @GetMapping("/eventos")
    @PreAuthorize("hasAuthority('SCOPE_audit_read') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Consultar eventos", description = "Consulta eventos de auditoria com filtros avançados")
    public Flux<EventoAuditoriaDto> consultarEventos(
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) LocalDateTime dataInicio,
            @RequestParam(required = false) LocalDateTime dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        
        return auditoriaService.consultarEventos(
            tipoEvento, entidade, usuario, dataInicio, dataFim, pageRequest
        );
    }

    /**
     * Obtém trilha completa de auditoria de uma entidade
     */
    @GetMapping("/trilha/{entidade}/{id}")
    @PreAuthorize("hasAuthority('SCOPE_audit_read') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Trilha de entidade", description = "Obtém trilha completa de auditoria de uma entidade específica")
    public Flux<EventoAuditoriaDto> obterTrilhaEntidade(
            @PathVariable String entidade,
            @PathVariable String id,
            Authentication authentication) {
        
        return auditoriaService.obterTrilhaEntidade(entidade, id);
    }

    /**
     * Verifica integridade de um evento específico
     */
    @GetMapping("/integridade/{eventoId}")
    @PreAuthorize("hasAuthority('SCOPE_audit_read') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Verificar integridade", description = "Verifica a integridade de um evento específico")
    public Mono<ResponseEntity<Map<String, Object>>> verificarIntegridade(
            @PathVariable String eventoId,
            Authentication authentication) {
        
        return auditoriaService.verificarIntegridade(eventoId)
                .map(resultado -> ResponseEntity.ok(resultado))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * Gera relatório de compliance
     */
    @GetMapping("/relatorio/compliance")
    @PreAuthorize("hasAuthority('SCOPE_compliance') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Relatório compliance", description = "Gera relatório de compliance para período específico")
    public Mono<ResponseEntity<RelatorioComplianceDto>> gerarRelatorioCompliance(
            @RequestParam LocalDateTime dataInicio,
            @RequestParam LocalDateTime dataFim,
            @RequestParam(required = false) String tipoRelatorio,
            Authentication authentication) {
        
        return auditoriaService.gerarRelatorioCompliance(dataInicio, dataFim, tipoRelatorio)
                .map(ResponseEntity::ok);
    }

    /**
     * Verifica assinatura digital de um evento
     */
    @PostMapping("/verificar-assinatura")
    @PreAuthorize("hasAuthority('SCOPE_audit_read') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Verificar assinatura", description = "Verifica assinatura digital de um evento")
    public Mono<ResponseEntity<Map<String, Object>>> verificarAssinatura(
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        String eventoId = (String) request.get("eventoId");
        String assinatura = (String) request.get("assinatura");
        
        return auditoriaService.verificarAssinatura(eventoId, assinatura)
                .map(resultado -> ResponseEntity.ok(Map.of(
                    "eventoId", eventoId,
                    "assinaturaValida", resultado.get("valida"),
                    "certificado", resultado.get("certificado"),
                    "dataVerificacao", LocalDateTime.now()
                )));
    }

    /**
     * Obtém métricas de auditoria
     */
    @GetMapping("/metricas")
    @PreAuthorize("hasAuthority('SCOPE_audit_read') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Métricas auditoria", description = "Obtém métricas agregadas de auditoria")
    public Mono<ResponseEntity<Map<String, Object>>> obterMetricas(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String tipo,
            Authentication authentication) {
        
        return auditoriaService.obterMetricas(periodo, tipo)
                .map(ResponseEntity::ok);
    }

    /**
     * Obtém estatísticas de integridade
     */
    @GetMapping("/estatisticas/integridade")
    @PreAuthorize("hasAuthority('SCOPE_audit_read') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Estatísticas integridade", description = "Obtém estatísticas de integridade do sistema")
    public Mono<ResponseEntity<Map<String, Object>>> obterEstatisticasIntegridade(
            Authentication authentication) {
        
        return auditoriaService.obterEstatisticasIntegridade()
                .map(ResponseEntity::ok);
    }

    /**
     * Valida cadeia de hashes
     */
    @PostMapping("/validar-cadeia")
    @PreAuthorize("hasAuthority('SCOPE_audit_read') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Validar cadeia", description = "Valida a cadeia completa de hashes de eventos")
    public Mono<ResponseEntity<Map<String, Object>>> validarCadeiaHashes(
            @RequestParam(required = false) LocalDateTime dataInicio,
            @RequestParam(required = false) LocalDateTime dataFim,
            Authentication authentication) {
        
        return auditoriaService.validarCadeiaHashes(dataInicio, dataFim)
                .map(resultado -> ResponseEntity.ok(Map.of(
                    "cadeiaValida", resultado.get("valida"),
                    "eventosVerificados", resultado.get("total"),
                    "eventosComFalha", resultado.get("falhas"),
                    "dataVerificacao", LocalDateTime.now()
                )));
    }

    /**
     * Obtém eventos por correlationId
     */
    @GetMapping("/correlacao/{correlationId}")
    @PreAuthorize("hasAuthority('SCOPE_audit_read') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Eventos por correlação", description = "Obtém todos os eventos relacionados a um correlationId")
    public Flux<EventoAuditoriaDto> obterEventosPorCorrelacao(
            @PathVariable String correlationId,
            Authentication authentication) {
        
        return auditoriaService.obterEventosPorCorrelacao(correlationId);
    }
}