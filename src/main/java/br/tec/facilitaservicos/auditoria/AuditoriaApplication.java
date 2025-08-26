package br.tec.facilitaservicos.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ============================================================================
 * 📋 MICROSERVIÇO DE AUDITORIA & COMPLIANCE - APLICAÇÃO PRINCIPAL
 * ============================================================================
 * 
 * Microserviço 100% reativo para auditoria e compliance:
 * - Event Sourcing com Axon Framework
 * - WORM Storage (Write-Once, Read-Many)
 * - Trilhas imutáveis de auditoria
 * - Integridade de dados com hashes encadeados
 * - Assinaturas digitais com Azure Key Vault
 * - GDPR compliance (Right to be forgotten)
 * - Retenção configurável por tipo de evento
 * - Event streaming com Kafka
 * - Consultas CQRS otimizadas
 * - Relatórios de compliance
 * 
 * Stack Tecnológica:
 * - Spring Boot 3.5+ com Java 24
 * - Spring WebFlux (100% reativo)
 * - R2DBC PostgreSQL (event store otimizado)
 * - Axon Framework (Event Sourcing + CQRS)
 * - Kafka (event streaming distribuído)
 * - BouncyCastle (criptografia + assinaturas)
 * - Azure Key Vault (gerenciamento de chaves)
 * - Redis (cache + read models)
 * - Micrometer + Prometheus (observabilidade)
 * 
 * Padrão Anti-extração:
 * - Feature flag: FEATURE_AUDITORIA_MS=false (default)
 * - Funcionalidade preservada no monólito
 * - Ativação gradual via configuração
 * - Rollback instantâneo
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootApplication
@EnableR2dbcAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class AuditoriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditoriaApplication.class, args);
    }
}