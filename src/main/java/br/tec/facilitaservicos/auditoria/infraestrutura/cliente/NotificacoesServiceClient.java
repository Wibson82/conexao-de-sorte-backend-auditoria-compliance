package br.tec.facilitaservicos.auditoria.infraestrutura.cliente;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 📧 CLIENTE NOTIFICAÇÕES - AUDITORIA & COMPLIANCE  
 * ============================================================================
 * 
 * Cliente reativo para integração com o microserviço de notificações.
 * Usado para enviar notificações quando eventos críticos de auditoria ocorrem.
 * 
 * Funcionalidades:
 * - Envio de notificações de eventos críticos de auditoria
 * - Alertas de compliance e violações de segurança
 * - Circuit Breaker para resiliência
 * - Retry automático com backoff exponencial
 * - Timeouts configuráveis
 * 
 * @author Sistema de Auditoria Reativo
 * @version 1.0
 * @since 2024
 */
@Component
public class NotificacoesServiceClient {
    
    private final WebClient webClient;
    private final String notificacoesBaseUrl;
    
    public NotificacoesServiceClient(@Value("${services.notificacoes.url:http://conexao-de-sorte-backend-notificacoes:8084}") String notificacoesBaseUrl,
                                   WebClient.Builder webClientBuilder) {
        this.notificacoesBaseUrl = notificacoesBaseUrl;
        this.webClient = webClientBuilder
            .baseUrl(notificacoesBaseUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
            .build();
    }
    
    /**
     * Enviar notificação de evento crítico de auditoria
     */
    @CircuitBreaker(name = "notificacoes-service", fallbackMethod = "fallbackNotificarEventoCritico")
    @Retry(name = "notificacoes-service") 
    @TimeLimiter(name = "notificacoes-service")
    public Mono<String> notificarEventoCritico(String eventoId, String tipoEvento, String usuarioId, String descricao) {
        var notificacao = Map.of(
            "tipo", "EVENTO_CRITICO_AUDITORIA",
            "destinatario", "admin@conexaodesorte.com.br",
            "assunto", "🚨 Evento Crítico de Auditoria: " + tipoEvento,
            "conteudo", String.format(
                "Evento crítico detectado:\n\n" +
                "ID do Evento: %s\n" + 
                "Tipo: %s\n" +
                "Usuário: %s\n" +
                "Descrição: %s\n\n" +
                "Verifique imediatamente no painel de auditoria.",
                eventoId, tipoEvento, usuarioId, descricao
            ),
            "prioridade", "ALTA",
            "contexto", Map.of(
                "eventoId", eventoId,
                "origem", "AUDITORIA_SERVICE"
            )
        );
        
        return webClient.post()
            .uri("/rest/v1/notificacoes/enviar")
            .bodyValue(notificacao)
            .retrieve()
            .onStatus(status -> status.isError(), response -> 
                response.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new RuntimeException("Erro ao enviar notificação: " + body)))
            )
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(10))
            .doOnNext(result -> 
                System.out.println("✅ Notificação de evento crítico enviada: " + eventoId)
            )
            .onErrorResume(WebClientResponseException.class, ex -> {
                System.err.println("❌ Erro HTTP ao notificar evento crítico: " + ex.getStatusCode());
                return Mono.just("ERRO_HTTP_" + ex.getStatusCode().value());
            });
    }
    
    /**
     * Enviar alerta de violação de compliance
     */
    @CircuitBreaker(name = "notificacoes-service", fallbackMethod = "fallbackNotificarViolacaoCompliance")
    @Retry(name = "notificacoes-service")
    @TimeLimiter(name = "notificacoes-service")
    public Mono<String> notificarViolacaoCompliance(String regulamento, String descricaoViolacao, String usuarioId) {
        var notificacao = Map.of(
            "tipo", "VIOLACAO_COMPLIANCE",
            "destinatario", "compliance@conexaodesorte.com.br",
            "assunto", "⚠️ Violação de Compliance Detectada: " + regulamento,
            "conteudo", String.format(
                "Violação de compliance detectada:\n\n" +
                "Regulamento: %s\n" +
                "Usuário Envolvido: %s\n" +
                "Descrição: %s\n\n" +
                "Ação imediata requerida para manter conformidade regulatória.",
                regulamento, usuarioId, descricaoViolacao
            ),
            "prioridade", "CRITICA",
            "contexto", Map.of(
                "regulamento", regulamento,
                "usuarioId", usuarioId,
                "origem", "COMPLIANCE_MONITOR"
            )
        );
        
        return webClient.post()
            .uri("/rest/v1/notificacoes/enviar")
            .bodyValue(notificacao)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(10))
            .doOnNext(result -> 
                System.out.println("✅ Alerta de compliance enviado para: " + regulamento)
            );
    }
    
    /**
     * Verificar status do serviço de notificações
     */
    public Mono<Boolean> verificarStatusServico() {
        return webClient.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(Map.class)
            .map(health -> "UP".equals(((Map<?, ?>) health).get("status")))
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn(false)
            .doOnNext(status -> 
                System.out.println(status ? "✅ Serviço de Notificações: UP" : "❌ Serviço de Notificações: DOWN")
            );
    }
    
    // ========== FALLBACK METHODS ==========
    
    /**
     * Fallback para notificação de evento crítico
     */
    private Mono<String> fallbackNotificarEventoCritico(String eventoId, String tipoEvento, String usuarioId, String descricao, Exception ex) {
        System.err.println("🔄 Fallback ativado para notificação crítica - Evento: " + eventoId + " - Erro: " + ex.getMessage());
        
        // Em produção, aqui poderia:
        // 1. Salvar em fila de retry
        // 2. Enviar por canal alternativo (SMS, Slack)
        // 3. Log em sistema de alertas
        
        return Mono.just("FALLBACK_CRITICO_" + eventoId);
    }
    
    /**
     * Fallback para notificação de violação de compliance
     */
    private Mono<String> fallbackNotificarViolacaoCompliance(String regulamento, String descricaoViolacao, String usuarioId, Exception ex) {
        System.err.println("🔄 Fallback ativado para compliance - Regulamento: " + regulamento + " - Erro: " + ex.getMessage());
        
        return Mono.just("FALLBACK_COMPLIANCE_" + regulamento);
    }
}