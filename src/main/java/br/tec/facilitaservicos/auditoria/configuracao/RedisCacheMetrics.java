package br.tec.facilitaservicos.auditoria.configuracao;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas customizadas para monitoramento do cache Redis.
 * Específico para padrões de acesso de auditoria e compliance.
 */
public class RedisCacheMetrics {

    private final RedisCacheManager cacheManager;
    private final MeterRegistry meterRegistry;
    private final String applicationName;
    
    // Contadores para métricas de auditoria
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheEvictionCounter;
    private final Timer cacheAccessTimer;
    private final AtomicLong totalAuditEvents;
    private final AtomicLong totalReportsInCache;
    private final AtomicLong complianceViolations;
    private final AtomicLong activeAuditSessions;

    public RedisCacheMetrics(RedisCacheManager cacheManager, MeterRegistry meterRegistry, String applicationName) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
        this.applicationName = applicationName;
        
        // Inicializar contadores específicos de auditoria
        this.cacheHitCounter = Counter.builder("redis.cache.hit")
                .description("Número de cache hits")
                .tag("service", applicationName)
                .tag("type", "audit")
                .register(meterRegistry);
                
        this.cacheMissCounter = Counter.builder("redis.cache.miss")
                .description("Número de cache misses")
                .tag("service", applicationName)
                .tag("type", "audit")
                .register(meterRegistry);
                
        this.cacheEvictionCounter = Counter.builder("redis.cache.eviction")
                .description("Número de evictions do cache")
                .tag("service", applicationName)
                .tag("type", "audit")
                .register(meterRegistry);
                
        this.cacheAccessTimer = Timer.builder("redis.cache.access")
                .description("Tempo de acesso ao cache")
                .tag("service", applicationName)
                .tag("type", "audit")
                .register(meterRegistry);

        this.totalAuditEvents = new AtomicLong(0);
        this.totalReportsInCache = new AtomicLong(0);
        this.complianceViolations = new AtomicLong(0);
        this.activeAuditSessions = new AtomicLong(0);
        
        // Gauges para métricas em tempo real específicas de auditoria
        Gauge.builder("audit.events.cached", totalAuditEvents, AtomicLong::doubleValue)
                .description("Total de eventos de auditoria em cache")
                .tag("service", applicationName)
                .register(meterRegistry);
                
        Gauge.builder("audit.reports.cached", totalReportsInCache, AtomicLong::doubleValue)
                .description("Total de relatórios em cache")
                .tag("service", applicationName)
                .register(meterRegistry);
                
        Gauge.builder("compliance.violations", complianceViolations, AtomicLong::doubleValue)
                .description("Violações de compliance detectadas")
                .tag("service", applicationName)
                .register(meterRegistry);
                
        Gauge.builder("audit.sessions.active", activeAuditSessions, AtomicLong::doubleValue)
                .description("Sessões de auditoria ativas")
                .tag("service", applicationName)
                .register(meterRegistry);
    }

    public RedisCacheManager instrumentedCacheManager() {
        return cacheManager;
    }

    public void recordCacheHit(String cacheName) {
        cacheHitCounter.increment();
    }

    public void recordCacheMiss(String cacheName) {
        cacheMissCounter.increment();
    }

    public void recordCacheEviction(String cacheName) {
        cacheEvictionCounter.increment();
    }

    public Timer.Sample startCacheAccess() {
        return Timer.start(meterRegistry);
    }

    public void recordCacheAccess(Timer.Sample sample) {
        sample.stop(cacheAccessTimer);
    }

    public void updateTotalAuditEvents(long count) {
        totalAuditEvents.set(count);
    }

    public void updateTotalReportsInCache(long count) {
        totalReportsInCache.set(count);
    }

    public void recordComplianceViolation() {
        complianceViolations.incrementAndGet();
    }

    public void updateActiveAuditSessions(long count) {
        activeAuditSessions.set(count);
    }

    /**
     * Coleta métricas específicas do Redis para auditoria
     */
    public void collectAuditSpecificMetrics(RedisTemplate<String, Object> redisTemplate) {
        try {
            // Contagem de chaves específicas de auditoria
            Long auditEventKeys = redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.eval("return #redis.call('keys', ARGV[1])".getBytes(), ReturnType.INTEGER, 1, (applicationName + ":audit:eventos:*").getBytes())
            );
            
            Long reportKeys = redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.eval("return #redis.call('keys', ARGV[1])".getBytes(), ReturnType.INTEGER, 1, (applicationName + ":audit:relatorios:*").getBytes())
            );
            
            Long sessionKeys = redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.eval("return #redis.call('keys', ARGV[1])".getBytes(), ReturnType.INTEGER, 1, (applicationName + ":audit:sessoes-auditoria:*").getBytes())
            );
            
            if (auditEventKeys != null) {
                updateTotalAuditEvents(auditEventKeys);
            }
            
            if (reportKeys != null) {
                updateTotalReportsInCache(reportKeys);
            }
            
            if (sessionKeys != null) {
                updateActiveAuditSessions(sessionKeys);
            }
            
        } catch (Exception e) {
            // Log do erro sem quebrar a aplicação
            System.err.println("Erro ao coletar métricas do Redis para auditoria: " + e.getMessage());
        }
    }
}