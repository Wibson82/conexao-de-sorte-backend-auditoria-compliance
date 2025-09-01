package br.tec.facilitaservicos.auditoria.aplicacao.servico;

import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;
import br.tec.facilitaservicos.auditoria.dominio.repositorio.EventoAuditoriaRepository;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ============================================================================
 * 🗄️ SERVIÇO DE CACHE PARA AUDITORIA 
 * ============================================================================
 * 
 * Gerencia cache Redis para eventos de auditoria e compliance:
 * - Cache inteligente de eventos frequentes
 * - Timeline cache para consultas rápidas
 * - Estratégias de invalidação automática
 * - Métricas de performance de cache
 * - TTL adaptativo baseado em padrões de acesso
 * - Cache warming para dados críticos
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ReactiveStringRedisTemplate stringRedisTemplate;
    private final EventoAuditoriaRepository repository;
    private final ObjectMapper objectMapper;

    // Cache TTL configurations
    private static final Duration TTL_EVENTO_RECENTE = Duration.ofMinutes(15);
    private static final Duration TTL_TIMELINE = Duration.ofMinutes(30);
    private static final Duration TTL_ESTATISTICAS = Duration.ofMinutes(5);
    private static final Duration TTL_EVENTO_ANTIGO = Duration.ofHours(2);
    
    // Timeout configurations for safety
    private static final Duration REDIS_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_CACHE_KEY_LENGTH = 250;

    // Cache key prefixes
    private static final String PREFIX_EVENTO = "audit:evento:";
    private static final String PREFIX_TIMELINE = "audit:timeline:";
    private static final String PREFIX_STATS = "audit:stats:";
    private static final String PREFIX_USER_EVENTS = "audit:user:";

    public CacheService(ReactiveRedisTemplate<String, Object> redisTemplate,
                       ReactiveStringRedisTemplate stringRedisTemplate,
                       EventoAuditoriaRepository repository,
                       ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Busca evento no cache primeiro, fallback para database
     */
    public Mono<EventoAuditoriaDto> buscarEventoCache(String eventoId) {
        if (eventoId == null || eventoId.trim().isEmpty()) {
            logger.warn("ID do evento não pode ser nulo ou vazio");
            return Mono.empty();
        }
        
        String cacheKey = PREFIX_EVENTO + eventoId;
        
        // Validação de segurança para evitar chaves muito longas
        if (cacheKey.length() > MAX_CACHE_KEY_LENGTH) {
            logger.warn("Chave de cache muito longa para evento: {}", eventoId);
            return repository.findById(eventoId).map(this::mapToDto);
        }
        
        return redisTemplate.opsForValue()
            .get(cacheKey)
            .cast(EventoAuditoriaDto.class)
            .timeout(REDIS_TIMEOUT)
            .doOnNext(_ -> logger.debug("Cache hit para evento: {}", eventoId))
            .switchIfEmpty(
                // Cache miss - buscar na database
                repository.findById(eventoId)
                    .map(this::mapToDto)
                    .doOnNext(evento -> {
                        logger.debug("Cache miss para evento: {}, carregando do banco", eventoId);
                        cachearEvento(eventoId, evento).subscribe();
                    })
            )
            .doOnError(error -> logger.error("Erro ao buscar evento no cache: {}", eventoId, error));
    }

    /**
     * Busca timeline de entidade no cache
     */
    public Flux<EventoAuditoriaDto> buscarTimelineCache(String entidadeTipo, String entidadeId) {
        if (entidadeTipo == null || entidadeTipo.trim().isEmpty() || 
            entidadeId == null || entidadeId.trim().isEmpty()) {
            logger.warn("Tipo de entidade e ID da entidade são obrigatórios");
            return Flux.empty();
        }
        
        String cacheKey = PREFIX_TIMELINE + entidadeTipo + ":" + entidadeId;
        
        // Validação de segurança para evitar chaves muito longas
        if (cacheKey.length() > MAX_CACHE_KEY_LENGTH) {
            logger.warn("Chave de cache muito longa para timeline: {}:{}", entidadeTipo, entidadeId);
            return repository.getTimelineEntidade(entidadeTipo, entidadeId).map(this::mapToDto);
        }
        
        return redisTemplate.opsForList()
            .range(cacheKey, 0, -1)
            .cast(EventoAuditoriaDto.class)
            .timeout(REDIS_TIMEOUT)
            .switchIfEmpty(
                // Cache miss - buscar timeline completa e cachear
                repository.getTimelineEntidade(entidadeTipo, entidadeId)
                    .map(this::mapToDto)
                    .collectList()
                    .doOnNext(timeline -> {
                        if (!timeline.isEmpty()) {
                            cachearTimeline(cacheKey, timeline).subscribe();
                        }
                    })
                    .flatMapMany(Flux::fromIterable)
            )
            .doOnError(error -> logger.error("Erro ao buscar timeline no cache: {}:{}", 
                entidadeTipo, entidadeId, error));
    }

    /**
     * Cacheia timeline de uma entidade
     */
    public Mono<Void> cachearTimeline(String cacheKey, Object timeline) {
        if (timeline instanceof java.util.List<?> timelineList) {
            return redisTemplate.opsForList()
                .rightPushAll(cacheKey, timelineList.toArray())
                .then(redisTemplate.expire(cacheKey, TTL_TIMELINE))
                .then();
        }
        return Mono.empty();
    }

    /**
     * Invalida todos os caches relacionados a um usuário
     */
    public Mono<Void> invalidarCachesUsuario(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("ID do usuário não pode ser nulo ou vazio para invalidar cache");
            return Mono.empty();
        }
        
        return stringRedisTemplate.keys(PREFIX_USER_EVENTS + userId + "*")
            .flatMap(stringRedisTemplate::delete)
            .then(Mono.from(stringRedisTemplate.keys(PREFIX_TIMELINE + "*:" + userId)
                .flatMap(stringRedisTemplate::delete)))
            .then()
            .doOnSuccess(_ -> logger.debug("Cache invalidado para usuário: {}", userId))
            .doOnError(error -> logger.error("Erro ao invalidar cache do usuário: {}", userId, error));
    }

    /**
     * Obtém estatísticas de performance do cache
     */
    @SuppressWarnings("unchecked")
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
     * Limpa o cache de auditoria
     */
    public Mono<Void> limparTodoCache() {
        return Flux.merge(
                stringRedisTemplate.keys(PREFIX_EVENTO + "*"),
                stringRedisTemplate.keys(PREFIX_TIMELINE + "*"),
                stringRedisTemplate.keys(PREFIX_STATS + "*"),
                stringRedisTemplate.keys(PREFIX_USER_EVENTS + "*")
            )
            .flatMap(stringRedisTemplate::delete)
            .then();
    }

    /**
     * Pre-aquece o cache com dados críticos
     */
    public Mono<Map<String, Object>> preAquecerCache() {
        return Mono.fromRunnable(() -> {
            // Cachear eventos críticos do dia atual
            repository.findEventosRecentes(24)
                .map(this::mapToDto)
                .doOnNext(evento -> 
                    cachearEvento(evento.id(), evento).subscribe()
                )
                .subscribe();

            // Cachear estatísticas frequentes
            gerarEstatisticasCache()
                .doOnNext(stats ->
                    redisTemplate.opsForValue()
                        .set(PREFIX_STATS + "performance", stats, TTL_ESTATISTICAS)
                        .subscribe()
                )
                .subscribe();
        })
        .then(Mono.just(Map.of(
            "status", "sucesso",
            "timestamp", LocalDateTime.now(),
            "operacao", "pre_aquecimento_cache"
        )));
    }

    /**
     * Cacheia evento individual com TTL inteligente
     */
    private Mono<Void> cachearEvento(String eventoId, EventoAuditoriaDto evento) {
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
     * Gera estatísticas atualizadas do cache
     */
    private Mono<Map<String, Object>> gerarEstatisticasCache() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            // Contar chaves por prefixo
            stats.put("eventos_cacheados", contarChaves(PREFIX_EVENTO));
            stats.put("timelines_cacheadas", contarChaves(PREFIX_TIMELINE));
            stats.put("usuarios_cacheados", contarChaves(PREFIX_USER_EVENTS));
            
            // Métricas de uso
            stats.put("memoria_utilizada_mb", obterUsoMemoriaRedis());
            stats.put("timestamp_coleta", LocalDateTime.now());
            stats.put("cache_hit_ratio", calcularHitRatio());
            
            // Status geral
            stats.put("status_cache", "operacional");
            stats.put("ttl_medio_segundos", calcularTtlMedio());
            
            return stats;
        });
    }

    /**
     * Converte entidade R2DBC para DTO
     */
    private EventoAuditoriaDto mapToDto(EventoAuditoriaR2dbc entidade) {
        Map<String, Object> metadados = null;
        try {
            if (entidade.getMetadados() != null && !entidade.getMetadados().isBlank()) {
                metadados = objectMapper.readValue(entidade.getMetadados(), 
                    new TypeReference<Map<String, Object>>() {});
            }
        } catch (JsonProcessingException ex) {
            logger.warn("Erro ao processar metadados do evento {}: {}", entidade.getId(), ex.getMessage());
            metadados = null;
        }
        
        return new EventoAuditoriaDto(
            entidade.getId(),
            entidade.getTipoEvento() != null ? entidade.getTipoEvento().toString() : "DESCONHECIDO",
            entidade.getDataEvento(),
            entidade.getUsuarioId(),
            entidade.getUsuarioNome(),
            entidade.getAcaoRealizada(),
            entidade.getEntidadeTipo(),
            entidade.getEntidadeId(),
            entidade.getEntidadeNome(),
            entidade.getStatusEvento() != null ? entidade.getStatusEvento().name() : "CRIADO",
            entidade.getSeveridade() != null ? entidade.getSeveridade().name() : "INFO",
            entidade.getIpOrigem(),
            entidade.getUserAgent(),
            metadados,
            entidade.getHashEvento(),
            entidade.getHashAnterior(),
            entidade.isDadosPessoais(),
            entidade.getCategoriaCompliance(),
            entidade.getRetencaoAte(),
            entidade.isAnonimizado(),
            entidade.getDataProcessamento() != null ? entidade.getDataProcessamento() : LocalDateTime.now()
        );
    }

    // ========== MÉTODOS AUXILIARES DE MÉTRICAS ==========

    private Mono<Long> contarChaves(String prefix) {
        return stringRedisTemplate.keys(prefix + "*")
            .count()
            .onErrorReturn(0L);
    }

    private Mono<Double> obterUsoMemoriaRedis() {
        // Usa o comando INFO memory do Redis para obter o uso real de memória
        return stringRedisTemplate.execute(connection -> 
            connection.serverCommands().info("memory")
        )
        .collectList()
        .map(list -> {
            if (list.isEmpty()) return 0.0;
            Properties info = list.get(0);
            String memoryInfo = info.getProperty("used_memory");
            if (memoryInfo != null) {
                try {
                    return Double.parseDouble(memoryInfo) / (1024 * 1024); // bytes para MB
                } catch (NumberFormatException e) {
                    logger.warn("Erro ao converter uso de memória: {}", memoryInfo, e);
                }
            }
            return 0.0;
        })
        .onErrorReturn(0.0);
    }

    private Mono<Double> calcularHitRatio() {
        // Usa o comando INFO stats do Redis para obter hits e misses reais
        return stringRedisTemplate.execute(connection -> 
            connection.serverCommands().info("stats")
        )
        .collectList()
        .map(list -> {
            if (list.isEmpty()) return 0.0;
            Properties info = list.get(0);
            long hits = 0;
            long misses = 0;
            
            String hitsStr = info.getProperty("keyspace_hits");
            String missesStr = info.getProperty("keyspace_misses");
            
            try {
                if (hitsStr != null) {
                    hits = Long.parseLong(hitsStr);
                }
                if (missesStr != null) {
                    misses = Long.parseLong(missesStr);
                }
            } catch (NumberFormatException e) {
                logger.warn("Erro ao converter estatísticas Redis: hits={}, misses={}", hitsStr, missesStr, e);
                return 0.0;
            }
            
            long total = hits + misses;
            return total == 0 ? 1.0 : (double) hits / total;
        })
        .onErrorReturn(0.0);
    }private int calcularTtlMedio() {
        // TTL médio baseado nas configurações
        return (int) ((TTL_EVENTO_RECENTE.toSeconds() + TTL_TIMELINE.toSeconds()) / 2);
    }
}