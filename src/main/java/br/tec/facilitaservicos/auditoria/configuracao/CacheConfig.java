package br.tec.facilitaservicos.auditoria.configuracao;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração de cache Redis otimizada para microserviço de auditoria e compliance.
 * TTLs conservadores para dados críticos de auditoria:
 * - Eventos: 6 horas (dados críticos com persistência longa)
 * - Relatórios: 2 horas (dados agregados)
 * - Metadados: 1 hora (configurações e definições)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.application.name:auditoria-compliance}")
    private String applicationName;

    // Cache names específicos do domínio de auditoria
    public static final String EVENTOS_CACHE = "audit:eventos";
    public static final String RELATORIOS_CACHE = "audit:relatorios";
    public static final String METADADOS_CACHE = "audit:metadados";
    public static final String CONFIGURACOES_COMPLIANCE_CACHE = "audit:configuracoes-compliance";
    public static final String USUARIOS_AUDITORIA_CACHE = "audit:usuarios-auditoria";
    public static final String SESSOES_AUDITORIA_CACHE = "audit:sessoes-auditoria";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        // Configuração base com serialização otimizada e segurança
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // TTL padrão conservador para auditoria
                .computePrefixWith(cacheName -> applicationName + ":" + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // TTLs diferenciados por criticidade e requisitos de compliance
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Eventos de auditoria - TTL longo para dados críticos
        cacheConfigurations.put(EVENTOS_CACHE, defaultConfig.entryTtl(Duration.ofHours(6)));
        
        // Relatórios - TTL moderado para dados agregados
        cacheConfigurations.put(RELATORIOS_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Metadados e configurações - TTL padrão
        cacheConfigurations.put(METADADOS_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CONFIGURACOES_COMPLIANCE_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Dados de sessão - TTL curto para segurança
        cacheConfigurations.put(USUARIOS_AUDITORIA_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(SESSOES_AUDITORIA_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        // Registrar métricas de cache personalizadas
        return new RedisCacheMetrics(cacheManager, meterRegistry, applicationName).instrumentedCacheManager();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Serialização otimizada para performance e compatibilidade
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        
        return template;
    }
}