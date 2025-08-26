package br.tec.facilitaservicos.auditoria.dominio.repositorio;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;
import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.StatusEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 📋 REPOSITÓRIO REATIVO DE EVENTOS DE AUDITORIA
 * ============================================================================
 * 
 * Repositório R2DBC para operações reativas com eventos de auditoria no MySQL 8.4:
 * 
 * Funcionalidades principais:
 * - CRUD reativo básico
 * - Consultas otimizadas por índices
 * - Filtros múltiplos combinados
 * - Agregações para relatórios
 * - Busca textual em JSON
 * - Timeline de eventos
 * - Compliance queries (LGPD/GDPR)
 * 
 * Otimizações MySQL 8.4:
 * - JSON functions para metadados
 * - Full-text search em descrições
 * - Particionamento por data
 * - Índices compostos otimizados
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Repository
public interface EventoAuditoriaRepository extends ReactiveCrudRepository<EventoAuditoriaR2dbc, String> {

    // === CONSULTAS BÁSICAS ===

    /**
     * Busca eventos por usuário com paginação
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE usuario_id = :usuarioId 
        ORDER BY data_evento DESC 
        LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
        """)
    Flux<EventoAuditoriaR2dbc> findByUsuarioId(@Param("usuarioId") String usuarioId, Pageable pageable);

    /**
     * Busca eventos por tipo de evento
     */
    Flux<EventoAuditoriaR2dbc> findByTipoEvento(TipoEvento tipoEvento, Pageable pageable);

    /**
     * Busca eventos por status
     */
    Flux<EventoAuditoriaR2dbc> findByStatusEvento(StatusEvento statusEvento, Pageable pageable);

    /**
     * Busca eventos por severidade
     */
    Flux<EventoAuditoriaR2dbc> findBySeveridade(NivelSeveridade severidade, Pageable pageable);

    // === CONSULTAS POR PERÍODO ===

    /**
     * Busca eventos em um período específico
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE data_evento BETWEEN :dataInicio AND :dataFim 
        ORDER BY data_evento DESC
        """)
    Flux<EventoAuditoriaR2dbc> findByPeriodo(
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim,
        Pageable pageable
    );

    /**
     * Busca eventos recentes (últimas N horas)
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE data_evento >= DATE_SUB(NOW(), INTERVAL :horas HOUR) 
        ORDER BY data_evento DESC
        """)
    Flux<EventoAuditoriaR2dbc> findEventosRecentes(@Param("horas") int horas);

    // === CONSULTAS POR ENTIDADE ===

    /**
     * Busca eventos relacionados a uma entidade específica
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE entidade_tipo = :entidadeTipo AND entidade_id = :entidadeId 
        ORDER BY data_evento DESC
        """)
    Flux<EventoAuditoriaR2dbc> findByEntidade(
        @Param("entidadeTipo") String entidadeTipo,
        @Param("entidadeId") String entidadeId,
        Pageable pageable
    );

    /**
     * Timeline completa de uma entidade
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE entidade_tipo = :entidadeTipo AND entidade_id = :entidadeId 
        ORDER BY data_evento ASC
        """)
    Flux<EventoAuditoriaR2dbc> getTimelineEntidade(
        @Param("entidadeTipo") String entidadeTipo,
        @Param("entidadeId") String entidadeId
    );

    // === CONSULTAS DE COMPLIANCE ===

    /**
     * Busca eventos que contêm dados pessoais de um usuário (LGPD)
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE dados_pessoais = true 
        AND (usuario_id = :usuarioId OR JSON_CONTAINS(metadados, JSON_OBJECT('cpf', :cpf))) 
        ORDER BY data_evento DESC
        """)
    Flux<EventoAuditoriaR2dbc> findDadosPessoaisUsuario(
        @Param("usuarioId") String usuarioId,
        @Param("cpf") String cpf
    );

    /**
     * Busca eventos expirados para política de retenção
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE retencao_ate < NOW() 
        AND status_evento != 'EXPIRADO' 
        ORDER BY data_evento ASC
        """)
    Flux<EventoAuditoriaR2dbc> findEventosExpirados();

    /**
     * Busca eventos pendentes de anonimização
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE dados_pessoais = true 
        AND anonimizado = false 
        AND categoria_compliance = 'DIREITO_ESQUECIMENTO'
        """)
    Flux<EventoAuditoriaR2dbc> findPendentesAnonimizacao();

    // === CONSULTAS DE SEGURANÇA ===

    /**
     * Busca tentativas de login falhadas por IP
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE tipo_evento = 'LOGIN_FALHA' 
        AND ip_origem = :ipOrigem 
        AND data_evento >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
        """)
    Flux<EventoAuditoriaR2dbc> findTentativasLoginPorIp(@Param("ipOrigem") String ipOrigem);

    /**
     * Busca eventos suspeitos de intrusão
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE severidade = 'CRITICAL' 
        AND tipo_evento IN ('TENTATIVA_INTRUSION', 'ACESSO_NEGADO', 'LOGIN_BLOQUEADO') 
        AND data_evento >= :dataInicio
        """)
    Flux<EventoAuditoriaR2dbc> findEventosSuspeitos(@Param("dataInicio") LocalDateTime dataInicio);

    // === AGREGAÇÕES E ESTATÍSTICAS ===

    /**
     * Conta eventos por tipo em um período
     */
    @Query("""
        SELECT tipo_evento, COUNT(*) as total 
        FROM eventos_auditoria 
        WHERE data_evento BETWEEN :dataInicio AND :dataFim 
        GROUP BY tipo_evento 
        ORDER BY total DESC
        """)
    Flux<Map<String, Object>> countEventosPorTipo(
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim
    );

    /**
     * Estatísticas de eventos por usuário
     */
    @Query("""
        SELECT usuario_id, usuario_nome, COUNT(*) as total_eventos, 
               MAX(data_evento) as ultimo_evento 
        FROM eventos_auditoria 
        WHERE data_evento >= :dataInicio 
        GROUP BY usuario_id, usuario_nome 
        ORDER BY total_eventos DESC 
        LIMIT 50
        """)
    Flux<Map<String, Object>> getEstatisticasUsuarios(@Param("dataInicio") LocalDateTime dataInicio);

    /**
     * Conta eventos por severidade hoje
     */
    @Query("""
        SELECT severidade, COUNT(*) as total 
        FROM eventos_auditoria 
        WHERE DATE(data_evento) = CURDATE() 
        GROUP BY severidade
        """)
    Flux<Map<String, Object>> countEventosPorSeveridadeHoje();

    // === BUSCA TEXTUAL ===

    /**
     * Busca textual em metadados JSON usando MySQL 8.4 JSON functions
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE JSON_SEARCH(metadados, 'one', :termo) IS NOT NULL 
        OR acao_realizada LIKE CONCAT('%', :termo, '%') 
        OR entidade_nome LIKE CONCAT('%', :termo, '%')
        ORDER BY data_evento DESC
        """)
    Flux<EventoAuditoriaR2dbc> buscarPorTexto(@Param("termo") String termo, Pageable pageable);

    // === INTEGRIDADE E HASH ===

    /**
     * Busca último evento para hash encadeado
     */
    @Query("""
        SELECT * FROM eventos_auditoria 
        WHERE hash_evento IS NOT NULL 
        ORDER BY data_evento DESC 
        LIMIT 1
        """)
    Mono<EventoAuditoriaR2dbc> findUltimoEventoComHash();

    /**
     * Verifica integridade da cadeia de hash
     */
    @Query("""
        SELECT COUNT(*) as total_quebras 
        FROM eventos_auditoria e1 
        JOIN eventos_auditoria e2 ON e2.hash_anterior = e1.hash_evento 
        WHERE e1.hash_evento != SHA2(CONCAT(e1.id, e1.tipo_evento, e1.data_evento), 256)
        """)
    Mono<Long> verificarIntegridadeHash();

    // === OPERAÇÕES DE LIMPEZA ===

    /**
     * Marca eventos como expirados
     */
    @Query("""
        UPDATE eventos_auditoria 
        SET status_evento = 'EXPIRADO' 
        WHERE retencao_ate < NOW() 
        AND status_evento != 'EXPIRADO'
        """)
    Mono<Integer> marcarEventosExpirados();

    /**
     * Remove eventos expirados (CUIDADO: operação destrutiva)
     */
    @Query("""
        DELETE FROM eventos_auditoria 
        WHERE status_evento = 'EXPIRADO' 
        AND data_evento < DATE_SUB(NOW(), INTERVAL 30 DAY)
        """)
    Mono<Integer> removerEventosExpirados();

    // === CONSULTAS DE MONITORAMENTO ===

    /**
     * Conta eventos críticos não processados
     */
    @Query("""
        SELECT COUNT(*) FROM eventos_auditoria 
        WHERE severidade IN ('ERROR', 'CRITICAL') 
        AND status_evento IN ('CRIADO', 'FALHA') 
        AND data_evento >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
        """)
    Mono<Long> countEventosCriticosNaoProcessados();

    /**
     * Health check do repositório
     */
    @Query("SELECT COUNT(*) FROM eventos_auditoria WHERE DATE(data_evento) = CURDATE()")
    Mono<Long> countEventosHoje();

    // === MÉTRICAS PARA DASHBOARD ===

    /**
     * Resumo executivo de eventos para dashboard
     */
    @Query("""
        SELECT 
            COUNT(*) as total_eventos,
            COUNT(CASE WHEN severidade = 'CRITICAL' THEN 1 END) as criticos,
            COUNT(CASE WHEN severidade = 'ERROR' THEN 1 END) as erros,
            COUNT(CASE WHEN dados_pessoais = true THEN 1 END) as dados_pessoais,
            COUNT(DISTINCT usuario_id) as usuarios_unicos
        FROM eventos_auditoria 
        WHERE data_evento >= :dataInicio
        """)
    Mono<Map<String, Object>> getResumoExecutivo(@Param("dataInicio") LocalDateTime dataInicio);
}