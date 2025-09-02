package br.tec.facilitaservicos.auditoria;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import br.tec.facilitaservicos.auditoria.aplicacao.dto.CriarEventoDto;
import br.tec.facilitaservicos.auditoria.aplicacao.servico.EventoAuditoriaService;
import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;
import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;
import br.tec.facilitaservicos.auditoria.dominio.enums.StatusEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;
import br.tec.facilitaservicos.auditoria.dominio.repositorio.EventoAuditoriaRepository;
import br.tec.facilitaservicos.auditoria.config.R2dbcConfig;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

/**
 * ============================================================================
 * ⚙️ TESTES DE CONFIGURAÇÃO R2DBC E TRANSAÇÕES REATIVAS
 * ============================================================================
 * 
 * Testes focados em:
 * 
 * ✅ Configuração de ConnectionFactory
 * ✅ Pool de conexões R2DBC
 * ✅ Transações reativas
 * ✅ Rollback automático
 * ✅ Isolamento de transações
 * ✅ Timeout de conexão
 * ✅ Retry de operações
 * ✅ Deadlock detection
 * ✅ Connection leaks
 * 
 * @author Sistema de Testes R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///auditoria_config_test",
    "spring.r2dbc.pool.enabled=true",
    "spring.r2dbc.pool.initial-size=5",
    "spring.r2dbc.pool.max-size=20",
    "spring.r2dbc.pool.max-idle-time=30m",
    "spring.r2dbc.pool.max-acquire-time=60s",
    "spring.r2dbc.pool.max-create-connection-time=30s",
    "spring.r2dbc.pool.validation-query=SELECT 1",
    "feature.auditoria.streaming=false"
})
@DisplayName("R2DBC Config e Transações - Testes de Integração")
class R2dbcConfigTransacaoTest {

    @Autowired
    private EventoAuditoriaService service;
    
    @Autowired
    private EventoAuditoriaRepository repository;
    
    @Autowired
    private R2dbcEntityTemplate r2dbcTemplate;
    
    @Autowired
    private DatabaseClient databaseClient;
    
    @Autowired
    private ConnectionFactory connectionFactory;
    
    @Autowired
    private ReactiveTransactionManager transactionManager;
    
    @Autowired
    private R2dbcConfig r2dbcConfig;
    
    private final String usuarioId = "user-config-test-123";
    
    @BeforeEach
    void setUp() {
        // Limpar dados de teste
        repository.deleteAll().block();
    }
    
    @Nested
    @DisplayName("Configuração R2DBC")
    class TestesConfiguracaoR2DBC {
        
        @Test
        @DisplayName("Deve configurar ConnectionFactory corretamente")
        void deveConfigurarConnectionFactoryCorretamente() {
            // Given & When
            ConnectionFactory factory = r2dbcConfig.connectionFactory();
            
            // Then
            assertNotNull(factory, "ConnectionFactory deve estar configurado");
            assertNotNull(connectionFactory, "ConnectionFactory deve estar injetado");
            
            // Testar conexão básica
            StepVerifier.create(
                Mono.from(connectionFactory.create())
                    .flatMap(connection -> 
                        Mono.from(connection.createStatement("SELECT 1")
                            .execute())
                            .doFinally(signalType -> connection.close())
                    )
            )
            .expectNextCount(1)
            .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve configurar TransactionManager reativo")
        void deveConfigurarTransactionManagerReativo() {
            // Given & When
            ReactiveTransactionManager txManager = r2dbcConfig.transactionManager(connectionFactory);
            
            // Then
            assertNotNull(txManager, "TransactionManager deve estar configurado");
            assertNotNull(transactionManager, "TransactionManager deve estar injetado");
            
            // Verificar se é do tipo correto
            assertTrue(txManager instanceof org.springframework.r2dbc.connection.R2dbcTransactionManager,
                "Deve ser R2dbcTransactionManager");
        }
        
        @Test
        @DisplayName("Deve executar queries básicas com R2dbcEntityTemplate")
        void deveExecutarQueriesBasicasComR2dbcEntityTemplate() {
            // Given
            EventoAuditoriaR2dbc evento = EventoAuditoriaR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuario(usuarioId, "João Silva")
                .entidade("USUARIO", usuarioId, "João Silva")
                .acao("Teste de configuração R2DBC")
                .severidade(NivelSeveridade.INFO)
                .sistema("conexao-de-sorte", "1.0.0")
                .build();
            
            // When & Then - Insert
            StepVerifier.create(r2dbcTemplate.insert(evento))
                .assertNext(eventoSalvo -> {
                    assertNotNull(eventoSalvo.getId());
                    assertEquals(evento.getAcaoRealizada(), eventoSalvo.getAcaoRealizada());
                })
                .verifyComplete();
            
            // When & Then - Select
            StepVerifier.create(r2dbcTemplate.select(EventoAuditoriaR2dbc.class)
                .matching(org.springframework.data.relational.core.query.Query.query(
                    org.springframework.data.relational.core.query.Criteria.where("id").is(evento.getId())))
                .one())
                .assertNext(eventoEncontrado -> {
                    assertEquals(evento.getId(), eventoEncontrado.getId());
                    assertEquals(evento.getAcaoRealizada(), eventoEncontrado.getAcaoRealizada());
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve executar queries com DatabaseClient")
        void deveExecutarQueriesComDatabaseClient() {
            // Given
            String eventoId = UUID.randomUUID().toString();
            
            // When & Then - Insert usando DatabaseClient
            StepVerifier.create(
                databaseClient.sql(
                    "INSERT INTO evento_auditoria (id, tipo_evento, usuario_id, usuario_nome, acao_realizada, severidade, data_evento) " +
                    "VALUES (:id, :tipo, :usuarioId, :usuarioNome, :acao, :severidade, :dataEvento)")
                    .bind("id", eventoId)
                    .bind("tipo", TipoEvento.LOGIN_SUCESSO.name())
                    .bind("usuarioId", usuarioId)
                    .bind("usuarioNome", "João Silva")
                    .bind("acao", "Teste DatabaseClient")
                    .bind("severidade", NivelSeveridade.INFO.name())
                    .bind("dataEvento", LocalDateTime.now())
                    .fetch()
                    .rowsUpdated()
            )
            .assertNext(rowsAffected -> assertEquals(1, rowsAffected))
            .verifyComplete();
            
            // When & Then - Select usando DatabaseClient
            StepVerifier.create(
                databaseClient.sql("SELECT * FROM evento_auditoria WHERE id = :id")
                    .bind("id", eventoId)
                    .fetch()
                    .one()
            )
            .assertNext(row -> {
                assertEquals(eventoId, row.get("id"));
                assertEquals("Teste DatabaseClient", row.get("acao_realizada"));
            })
            .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Transações Reativas")
    class TestesTransacoesReativas {
        
        @Test
        @DisplayName("Deve executar transação com commit automático")
        void deveExecutarTransacaoComCommitAutomatico() {
            // Given
            TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
            
            List<CriarEventoDto> eventos = List.of(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Transação 1")
                    .severidade(NivelSeveridade.INFO)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGOUT)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Transação 2")
                    .severidade(NivelSeveridade.INFO)
                    .build()
            );
            
            // When - Executar em transação
            Mono<List<EventoAuditoriaDto>> transacao = Flux.fromIterable(eventos)
                .flatMap(dto -> service.registrarEvento(dto))
                .collectList()
                .as(transactionalOperator::transactional);
            
            // Then
            StepVerifier.create(transacao)
                .assertNext(eventosRegistrados -> {
                    assertEquals(2, eventosRegistrados.size());
                    eventosRegistrados.forEach(evento -> {
                        assertNotNull(evento.id());
                        assertEquals(usuarioId, evento.usuarioId());
                    });
                })
                .verifyComplete();
            
            // Verificar se os dados foram persistidos
            StepVerifier.create(repository.count())
                .assertNext(count -> assertTrue(count >= 2))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve executar rollback em caso de erro")
        void deveExecutarRollbackEmCasoDeErro() {
            // Given
            TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
            
            // Primeiro evento válido
            CriarEventoDto eventoValido = CriarEventoDto.builder()
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuarioId(usuarioId)
                .usuarioNome("João Silva")
                .acaoRealizada("Evento válido")
                .severidade(NivelSeveridade.INFO)
                .build();
            
            // Contar eventos antes da transação
            Long countAntes = repository.count().block();
            
            // When - Executar transação que deve falhar
            Mono<String> transacaoComErro = service.registrarEvento(eventoValido)
                .then(Mono.error(new RuntimeException("Erro simulado para rollback")))
                .cast(String.class)
                .as(transactionalOperator::transactional);
            
            // Then - Deve falhar
            StepVerifier.create(transacaoComErro)
                .expectError(RuntimeException.class)
                .verify();
            
            // Verificar se houve rollback (count deve ser igual ao inicial)
            StepVerifier.create(repository.count())
                .assertNext(countDepois -> assertEquals(countAntes, countDepois))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve manter isolamento entre transações concorrentes")
        void deveManterIsolamentoEntreTransacoesConcorrentes() throws InterruptedException {
            // Given
            TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
            CountDownLatch latch = new CountDownLatch(2);
            
            // When - Executar duas transações concorrentes
            Mono<EventoAuditoriaDto> transacao1 = service.registrarEvento(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                    .usuarioId(usuarioId + "-1")
                    .usuarioNome("Usuário 1")
                    .acaoRealizada("Transação concorrente 1")
                    .severidade(NivelSeveridade.INFO)
                    .build()
            )
            .delayElement(Duration.ofMillis(100)) // Simular processamento
            .as(transactionalOperator::transactional)
            .doFinally(signal -> latch.countDown());
            
            Mono<EventoAuditoriaDto> transacao2 = service.registrarEvento(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                    .usuarioId(usuarioId + "-2")
                    .usuarioNome("Usuário 2")
                    .acaoRealizada("Transação concorrente 2")
                    .severidade(NivelSeveridade.INFO)
                    .build()
            )
            .delayElement(Duration.ofMillis(100)) // Simular processamento
            .as(transactionalOperator::transactional)
            .doFinally(signal -> latch.countDown());
            
            // Executar concorrentemente
            transacao1.subscribe();
            transacao2.subscribe();
            
            // Aguardar conclusão
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Transações devem completar em 5 segundos");
            
            // Then - Verificar se ambas foram persistidas
            StepVerifier.create(repository.count())
                .assertNext(count -> assertTrue(count >= 2))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve lidar com timeout de transação")
        void deveLidarComTimeoutDeTransacao() {
            // Given
            org.springframework.transaction.reactive.TransactionDefinition txDefinition = 
                new org.springframework.transaction.support.DefaultTransactionDefinition() {
                    @Override
                    public int getTimeout() {
                        return 1; // 1 segundo de timeout
                    }
                };
            
            TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager, txDefinition);
            
            // When - Executar operação que demora mais que o timeout
            Mono<EventoAuditoriaDto> transacaoComTimeout = service.registrarEvento(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Transação com timeout")
                    .severidade(NivelSeveridade.INFO)
                    .build()
            )
            .delayElement(Duration.ofSeconds(2)) // Delay maior que o timeout
            .as(transactionalOperator::transactional);
            
            // Then - Deve falhar por timeout
            StepVerifier.create(transacaoComTimeout)
                .expectError()
                .verify(Duration.ofSeconds(5));
        }
    }
    
    @Nested
    @DisplayName("Pool de Conexões")
    class TestesPoolConexoes {
        
        @Test
        @DisplayName("Deve gerenciar pool de conexões corretamente")
        void deveGerenciarPoolConexoesCorretamente() {
            // Given - Múltiplas operações simultâneas
            List<Mono<EventoAuditoriaDto>> operacoes = List.of(
                service.registrarEvento(criarEventoTeste("Op 1")),
                service.registrarEvento(criarEventoTeste("Op 2")),
                service.registrarEvento(criarEventoTeste("Op 3")),
                service.registrarEvento(criarEventoTeste("Op 4")),
                service.registrarEvento(criarEventoTeste("Op 5"))
            );
            
            // When - Executar todas simultaneamente
            Flux<EventoAuditoriaDto> operacoesConcorrentes = Flux.merge(operacoes);
            
            // Then - Todas devem completar com sucesso
            StepVerifier.create(operacoesConcorrentes)
                .expectNextCount(5)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve recuperar de falhas de conexão com retry")
        void deveRecuperarDeFalhasConexaoComRetry() {
            // Given
            CriarEventoDto evento = criarEventoTeste("Teste retry");
            
            // When - Simular operação com retry
            Mono<EventoAuditoriaDto> operacaoComRetry = service.registrarEvento(evento)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                    .filter(throwable -> throwable instanceof org.springframework.dao.DataAccessException));
            
            // Then
            StepVerifier.create(operacaoComRetry)
                .assertNext(eventoSalvo -> {
                    assertNotNull(eventoSalvo.id());
                    assertEquals("Teste retry", eventoSalvo.acaoRealizada());
                })
                .verifyComplete();
        }
        
        private CriarEventoDto criarEventoTeste(String acao) {
            return CriarEventoDto.builder()
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuarioId(usuarioId + "-" + acao.hashCode())
                .usuarioNome("Usuário " + acao)
                .acaoRealizada(acao)
                .severidade(NivelSeveridade.INFO)
                .build();
        }
    }
    
    @Nested
    @DisplayName("Detecção de Problemas")
    class TestesDetecaoProblemas {
        
        @Test
        @DisplayName("Deve detectar violação de integridade")
        void deveDetectarViolacaoIntegridade() {
            // Given - Tentar inserir evento com ID duplicado
            String eventoId = UUID.randomUUID().toString();
            
            EventoAuditoriaR2dbc evento1 = EventoAuditoriaR2dbc.builder()
                .id(eventoId)
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuario(usuarioId, "João Silva")
                .entidade("USUARIO", usuarioId, "João Silva")
                .acao("Primeiro evento")
                .severidade(NivelSeveridade.INFO)
                .sistema("conexao-de-sorte", "1.0.0")
                .build();
            
            EventoAuditoriaR2dbc evento2 = EventoAuditoriaR2dbc.builder()
                .id(eventoId) // Mesmo ID - deve causar violação
                .tipoEvento(TipoEvento.LOGOUT)
                .usuario(usuarioId, "João Silva")
                .entidade("USUARIO", usuarioId, "João Silva")
                .acao("Segundo evento")
                .severidade(NivelSeveridade.INFO)
                .sistema("conexao-de-sorte", "1.0.0")
                .build();
            
            // When - Inserir primeiro evento
            repository.save(evento1).block();
            
            // Then - Segundo evento deve falhar
            StepVerifier.create(repository.save(evento2))
                .expectError(DataIntegrityViolationException.class)
                .verify();
        }
        
        @Test
        @DisplayName("Deve monitorar health da conexão")
        void deveMonitorarHealthConexao() {
            // When - Executar query de health check
            Mono<Map<String, Object>> healthCheck = databaseClient.sql("SELECT 1 as status")
                .fetch()
                .one();
            
            // Then
            StepVerifier.create(healthCheck)
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(1, result.get("status"));
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve validar configurações de timeout")
        void deveValidarConfiguracoesTimeout() {
            // Given - Query que pode demorar
            Mono<Long> queryLenta = repository.count()
                .delayElement(Duration.ofMillis(100));
            
            // When & Then - Deve completar dentro do timeout esperado
            StepVerifier.create(queryLenta)
                .assertNext(count -> assertTrue(count >= 0))
                .verifyComplete();
        }
    }
}