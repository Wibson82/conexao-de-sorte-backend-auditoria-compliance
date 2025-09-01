package br.tec.facilitaservicos.auditoria.infraestrutura.cache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 📦 CACHE SERVICE - AUDITORIA & COMPLIANCE
 * ============================================================================ 
 * 
 * Serviço completo de cache Redis para auditoria:
 * - Cache inteligente de eventos com TTL dinâmico
 * - Timeline caching para consultas rápidas
 * - Invalidação seletiva por usuário
 * - Estatísticas de performance
 * - Pre-warming de dados críticos
 * 
 * Padrões implementados:
 * - 100% Reativo com WebFlux
 * - Redis como cache distribuído
 * - TTL inteligente baseado na idade dos eventos
 * - Invalidação cascata
 * - Métricas de cache hit/miss
 * 
 * @author Sistema de Auditoria Reativo
 * @version 1.0
 * @since 2024
 */
@Service
public class AuditoriaCacheService {
    
    // Cache Keys Patterns
    private static final String PREFIX_EVENTO = "audit:evento:";
    private static final String PREFIX_TIMELINE = "audit:timeline:";
    private static final String PREFIX_USER_EVENTS = "audit:user:";
    private static final String PREFIX_STATS = "audit:stats:";
    
    // TTL Configurations
    private static final Duration TTL_EVENTO_RECENTE = Duration.ofMinutes(15);
    private static final Duration TTL_EVENTO_ANTIGO = Duration.ofHours(2);
    private static final Duration TTL_TIMELINE = Duration.ofMinutes(10);
    private static final Duration TTL_ESTATISTICAS = Duration.ofMinutes(5);
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ReactiveStringRedisTemplate stringRedisTemplate;
    private final Map<String, Long> cacheStats = new ConcurrentHashMap<>();
    
    public AuditoriaCacheService(ReactiveRedisTemplate<String, Object> redisTemplate,
                                ReactiveStringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        
        // Inicializar estatísticas
        cacheStats.put("hits", 0L);
        cacheStats.put("misses", 0L);
        cacheStats.put("evictions", 0L);
    }
    
    /**
     * Buscar evento individual no cache
     */
    public Mono<EventoAuditoriaDto> buscarEventoCache(String eventoId) {
        String cacheKey = PREFIX_EVENTO + eventoId;
        
        return redisTemplate.opsForValue()
            .get(cacheKey)
            .cast(EventoAuditoriaDto.class)
            .doOnNext(evento -> incrementarStat("hits"))
            .switchIfEmpty(Mono.fromRunnable(() -> incrementarStat("misses")));
    }
    
    /**
     * Buscar timeline de eventos no cache
     */
    public Flux<EventoAuditoriaDto> buscarTimelineCache(String cacheKey) {
        String timelineKey = PREFIX_TIMELINE + cacheKey;
        
        return redisTemplate.opsForList()
            .range(timelineKey, 0, -1)
            .cast(EventoAuditoriaDto.class)
            .doOnComplete(() -> incrementarStat("hits"))
            .switchIfEmpty(Flux.<EventoAuditoriaDto>empty()
                .doOnComplete(() -> incrementarStat("misses")));
    }
    
    /**
     * Cachear timeline de eventos com TTL otimizado
     */
    public Mono<Void> cachearTimeline(String cacheKey, List<EventoAuditoriaDto> eventos) {
        String timelineKey = PREFIX_TIMELINE + cacheKey;
        
        if (eventos == null || eventos.isEmpty()) {
            return Mono.empty();
        }
        
        return redisTemplate.opsForList()
            .rightPushAll(timelineKey, eventos.toArray())
            .then(redisTemplate.expire(timelineKey, TTL_TIMELINE))
            .then();
    }
    
    /**
     * Cachear evento individual com TTL inteligente
     */
    public Mono<Void> cachearEvento(String eventoId, EventoAuditoriaDto evento) {
        String cacheKey = PREFIX_EVENTO + eventoId;
        
        // TTL baseado na idade do evento
        Duration ttl = evento.timestamp().isAfter(LocalDateTime.now().minusHours(1)) 
            ? TTL_EVENTO_RECENTE 
            : TTL_EVENTO_ANTIGO;
            
        return redisTemplate.opsForValue()
            .set(cacheKey, evento, ttl)
            .then();
    }
    
    /**
     * Invalidar todos os caches relacionados a um usuário
     */
    public Mono<Void> invalidarCachesUsuario(String userId) {
        return stringRedisTemplate.keys(PREFIX_USER_EVENTS + userId + "*")
            .flatMap(stringRedisTemplate::delete)
            .then(stringRedisTemplate.keys(PREFIX_TIMELINE + "*:" + userId)
                .flatMap(stringRedisTemplate::delete)
                .then())
            .then(Mono.fromRunnable(() -> incrementarStat("evictions")))
            .then();
    }
    
    /**
     * Obter estatísticas de performance do cache
     */
    public Mono<Map<String, Object>> obterEstatisticasCache() {
        String statsKey = PREFIX_STATS + "performance";
        
        return redisTemplate.opsForValue()
            .get(statsKey)
            .cast(Map.class)
            .map(map -> (Map<String, Object>) map)
            .switchIfEmpty(
                gerarEstatisticasCache()
                    .doOnNext(stats -> 
                        redisTemplate.opsForValue()
                            .set(statsKey, stats, TTL_ESTATISTICAS)
                            .subscribe()
                    )
            );
    }
    
    /**
     * Limpar todo o cache de auditoria
     */
    public Mono<Void> limparTodoCache() {
        return stringRedisTemplate.keys("audit:*")
            .flatMap(stringRedisTemplate::delete)
            .then(Mono.fromRunnable(() -> {
                cacheStats.put("hits", 0L);
                cacheStats.put("misses", 0L);
                cacheStats.put("evictions", 0L);
            }))
            .then();
    }
    
    /**
     * Pre-aquecer cache com eventos críticos
     */
    public Mono<Void> preAquecerCache(List<EventoAuditoriaDto> eventosCriticos) {
        return Flux.fromIterable(eventosCriticos)
            .flatMap(evento -> cachearEvento(evento.id(), evento))
            .then();
    }
    
    /**
     * Gerar estatísticas de cache em tempo real
     */
    private Mono<Map<String, Object>> gerarEstatisticasCache() {
        return Mono.fromCallable(() -> {
            long hits = cacheStats.getOrDefault("hits", 0L);
            long misses = cacheStats.getOrDefault("misses", 0L);
            long total = hits + misses;
            double hitRatio = total > 0 ? (double) hits / total * 100.0 : 0.0;
            
            return Map.of(
                "cache_hits", hits,
                "cache_misses", misses,
                "hit_ratio_percent", String.format("%.2f", hitRatio),
                "evictions", cacheStats.getOrDefault("evictions", 0L),
                "total_requests", total,
                "timestamp", LocalDateTime.now().toString()
            );
        });
    }
    
    /**
     * Incrementar contador de estatística
     */
    private void incrementarStat(String stat) {
        cacheStats.compute(stat, (key, value) -> value == null ? 1L : value + 1L);
    }
}