package br.tec.facilitaservicos.auditoria.aplicacao.servico;

import br.tec.facilitaservicos.auditoria.dominio.repositorio.EventoAuditoriaRepository;
import br.tec.facilitaservicos.auditoria.infraestrutura.cache.AuditoriaCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    private final AuditoriaCacheService auditoriaCacheService;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;

    public CacheWarmingService(AuditoriaCacheService auditoriaCacheService, EventoAuditoriaRepository eventoAuditoriaRepository) {
        this.auditoriaCacheService = auditoriaCacheService;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        log.info("🔥 Iniciando aquecimento do cache de auditoria...");

        // Exemplo de aquecimento: carregar os 100 eventos mais recentes para o cache
        // Isso pode ser otimizado para carregar dados mais relevantes ou frequentemente acessados
        eventoAuditoriaRepository.findEventosRecentes(24) // Buscar eventos das últimas 24 horas
                .collectList()
                .flatMap(eventos -> {
                    if (!eventos.isEmpty()) {
                        // Usar um userId dummy ou um identificador genérico para o cache de warming
                        // Ou criar um método específico no CacheService para salvar eventos sem userId
                        log.info("Aquecendo cache com {} eventos recentes.", eventos.size());
                        return auditoriaCacheService.salvarEventosTimeline("warming-user", eventos.stream().map(auditoriaCacheService::mapToDto).collect(java.util.stream.Collectors.toList()));
                    } else {
                        log.info("Nenhum evento recente para aquecer o cache.");
                        return Mono.empty();
                    }
                })
                .timeout(Duration.ofSeconds(30)) // Timeout para o processo de aquecimento
                .doOnError(e -> log.error("Erro durante o aquecimento do cache: {}", e.getMessage(), e))
                .doOnSuccess(v -> log.info("✅ Aquecimento do cache de auditoria concluído."))
                .subscribe();
    }
}
