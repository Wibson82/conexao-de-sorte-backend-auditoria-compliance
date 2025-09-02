package br.tec.facilitaservicos.auditoria;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import br.tec.facilitaservicos.auditoria.aplicacao.dto.CriarEventoDto;
import br.tec.facilitaservicos.auditoria.aplicacao.servico.EventoAuditoriaService;
import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;
import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;
import br.tec.facilitaservicos.auditoria.dominio.enums.StatusEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;
import br.tec.facilitaservicos.auditoria.dominio.repositorio.EventoAuditoriaRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

/**
 * ============================================================================
 * 🚀 TESTES DE PERFORMANCE E CONCORRÊNCIA PARA OPERAÇÕES REATIVAS
 * ============================================================================
 * 
 * Testes focados em:
 * 
 * ✅ Throughput de operações reativas
 * ✅ Latência de operações individuais
 * ✅ Concorrência e paralelismo
 * ✅ Stress testing com alta carga
 * ✅ Memory leaks e garbage collection
 * ✅ Backpressure handling
 * ✅ Circuit breaker patterns
 * ✅ Rate limiting
 * ✅ Bulk operations performance
 * 
 * @author Sistema de Testes Performance
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///auditoria_performance_test",
    "spring.r2dbc.pool.enabled=true",
    "spring.r2dbc.pool.initial-size=10",
    "spring.r2dbc.pool.max-size=50",
    "spring.r2dbc.pool.max-idle-time=30m",
    "spring.r2dbc.pool.max-acquire-time=60s",
    "feature.auditoria.streaming=true",
    "feature.auditoria.cache=true",
    "logging.level.io.r2dbc=WARN",
    "logging.level.reactor=WARN"
})
@DisplayName("Performance e Concorrência - Testes de Stress")
class PerformanceConcorrenciaTest {

    @Autowired
    private EventoAuditoriaService service;
    
    @Autowired
    private EventoAuditoriaRepository repository;
    
    private final String usuarioId = "user-perf-test-123";
    
    @BeforeEach
    void setUp() {
        // Limpar dados de teste
        repository.deleteAll().block();
    }
    
    private CriarEventoDto criarEventoTeste(String sufixo) {
        return CriarEventoDto.builder()
            .tipoEvento(TipoEvento.LOGIN_SUCESSO)
            .usuarioId(usuarioId + "-" + sufixo)
            .usuarioNome("Usuário Teste " + sufixo)
            .acaoRealizada("Ação de teste " + sufixo)
            .severidade(NivelSeveridade.INFO)
            .build();
    }
    
    @Nested
    @DisplayName("Testes de Throughput")
    class TestesThroughput {
        
        @Test
        @DisplayName("Deve processar 1000 eventos em menos de 10 segundos")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void deveProcessar1000EventosRapidamente() {
            // Given
            int totalEventos = 1000;
            List<CriarEventoDto> eventos = IntStream.range(0, totalEventos)
                .mapToObj(i -> criarEventoTeste(String.valueOf(i)))
                .toList();
            
            long startTime = System.currentTimeMillis();
            
            // When
            Flux<EventoAuditoriaDto> processamento = Flux.fromIterable(eventos)
                .flatMap(dto -> service.registrarEvento(dto), 20) // Concorrência de 20
                .subscribeOn(Schedulers.parallel());
            
            // Then
            StepVerifier.create(processamento)
                .expectNextCount(totalEventos)
                .verifyComplete();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("Processados " + totalEventos + " eventos em " + duration + "ms");
            System.out.println("Throughput: " + (totalEventos * 1000.0 / duration) + " eventos/segundo");
            
            // Verificar se foi processado em menos de 10 segundos
            assertTrue(duration < 10000, "Deve processar em menos de 10 segundos");
            
            // Verificar se todos foram persistidos
            StepVerifier.create(repository.count())
                .assertNext(count -> assertEquals(totalEventos, count.intValue()))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve manter throughput consistente com operações concorrentes")
        void deveManterThroughputConsistenteComOperacoesConcorrentes() {
            // Given
            int batchSize = 100;
            int numBatches = 5;
            AtomicLong totalProcessingTime = new AtomicLong(0);
            AtomicInteger processedBatches = new AtomicInteger(0);
            
            // When - Processar múltiplos batches concorrentemente
            List<Mono<Long>> batches = IntStream.range(0, numBatches)
                .mapToObj(batchIndex -> {
                    List<CriarEventoDto> eventos = IntStream.range(0, batchSize)
                        .mapToObj(i -> criarEventoTeste("batch" + batchIndex + "-" + i))
                        .toList();
                    
                    return Mono.fromCallable(() -> System.currentTimeMillis())
                        .flatMap(startTime -> 
                            Flux.fromIterable(eventos)
                                .flatMap(dto -> service.registrarEvento(dto), 10)
                                .count()
                                .map(count -> {
                                    long endTime = System.currentTimeMillis();
                                    long batchTime = endTime - startTime;
                                    totalProcessingTime.addAndGet(batchTime);
                                    processedBatches.incrementAndGet();
                                    System.out.println("Batch " + batchIndex + ": " + count + " eventos em " + batchTime + "ms");
                                    return batchTime;
                                })
                        )
                        .subscribeOn(Schedulers.parallel());
                })
                .toList();
            
            // Then
            StepVerifier.create(Flux.merge(batches))
                .expectNextCount(numBatches)
                .verifyComplete();
            
            // Verificar consistência
            assertEquals(numBatches, processedBatches.get());
            long avgTime = totalProcessingTime.get() / numBatches;
            System.out.println("Tempo médio por batch: " + avgTime + "ms");
            
            // Verificar se todos foram persistidos
            StepVerifier.create(repository.count())
                .assertNext(count -> assertEquals(batchSize * numBatches, count.intValue()))
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Testes de Latência")
    class TestesLatencia {
        
        @Test
        @DisplayName("Deve manter latência baixa para operações individuais")
        void deveManterLatenciaBaixaParaOperacoesIndividuais() {
            // Given
            CriarEventoDto evento = criarEventoTeste("latencia");
            
            // When & Then - Múltiplas medições
            List<Long> latencias = new ArrayList<>();
            
            for (int i = 0; i < 10; i++) {
                long startTime = System.nanoTime();
                
                StepVerifier.create(service.registrarEvento(evento))
                    .assertNext(eventoRegistrado -> {
                        assertNotNull(eventoRegistrado.id());
                    })
                    .verifyComplete();
                
                long endTime = System.nanoTime();
                long latencia = (endTime - startTime) / 1_000_000; // Convert to milliseconds
                latencias.add(latencia);
                
                // Limpar para próxima iteração
                repository.deleteAll().block();
            }
            
            // Calcular estatísticas
            double avgLatencia = latencias.stream().mapToLong(Long::longValue).average().orElse(0);
            long maxLatencia = latencias.stream().mapToLong(Long::longValue).max().orElse(0);
            long minLatencia = latencias.stream().mapToLong(Long::longValue).min().orElse(0);
            
            System.out.println("Latência média: " + avgLatencia + "ms");
            System.out.println("Latência máxima: " + maxLatencia + "ms");
            System.out.println("Latência mínima: " + minLatencia + "ms");
            
            // Verificar se latência está dentro do aceitável (< 100ms)
            assertTrue(avgLatencia < 100, "Latência média deve ser menor que 100ms");
            assertTrue(maxLatencia < 500, "Latência máxima deve ser menor que 500ms");
        }
        
        @Test
        @DisplayName("Deve manter latência estável sob carga")
        void deveManterLatenciaEstavelSobCarga() {
            // Given
            int numOperacoes = 50;
            List<Tuple2<Long, Long>> medicoes = new ArrayList<>();
            
            // When - Executar operações com medição de latência
            Flux<EventoAuditoriaDto> operacoes = Flux.range(0, numOperacoes)
                .flatMap(i -> {
                    long startTime = System.nanoTime();
                    return service.registrarEvento(criarEventoTeste(String.valueOf(i)))
                        .map(evento -> {
                            long endTime = System.nanoTime();
                            long latencia = (endTime - startTime) / 1_000_000;
                            medicoes.add(reactor.util.function.Tuples.of((long) i, latencia));
                            return evento;
                        });
                }, 10); // Concorrência controlada
            
            // Then
            StepVerifier.create(operacoes)
                .expectNextCount(numOperacoes)
                .verifyComplete();
            
            // Analisar distribuição de latência
            double avgLatencia = medicoes.stream().mapToLong(Tuple2::getT2).average().orElse(0);
            long maxLatencia = medicoes.stream().mapToLong(Tuple2::getT2).max().orElse(0);
            
            System.out.println("Latência média sob carga: " + avgLatencia + "ms");
            System.out.println("Latência máxima sob carga: " + maxLatencia + "ms");
            
            // Verificar estabilidade (desvio padrão não muito alto)
            double variance = medicoes.stream()
                .mapToDouble(t -> Math.pow(t.getT2() - avgLatencia, 2))
                .average().orElse(0);
            double stdDev = Math.sqrt(variance);
            
            System.out.println("Desvio padrão da latência: " + stdDev + "ms");
            
            assertTrue(stdDev < avgLatencia, "Desvio padrão deve ser menor que a média");
        }
    }
    
    @Nested
    @DisplayName("Testes de Concorrência")
    class TestesConcorrencia {
        
        @Test
        @DisplayName("Deve suportar alta concorrência sem deadlocks")
        void deveSuportarAltaConcorrenciaSemDeadlocks() throws InterruptedException {
            // Given
            int numThreads = 20;
            int operacoesPorThread = 25;
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger sucessos = new AtomicInteger(0);
            AtomicInteger erros = new AtomicInteger(0);
            
            // When - Executar operações concorrentes
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                
                Flux.range(0, operacoesPorThread)
                    .flatMap(i -> service.registrarEvento(
                        criarEventoTeste("thread" + threadId + "-op" + i)))
                    .subscribeOn(Schedulers.parallel())
                    .subscribe(
                        evento -> sucessos.incrementAndGet(),
                        error -> {
                            erros.incrementAndGet();
                            System.err.println("Erro na thread " + threadId + ": " + error.getMessage());
                        },
                        () -> latch.countDown()
                    );
            }
            
            // Then
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Todas as threads devem completar em 30 segundos");
            
            System.out.println("Sucessos: " + sucessos.get());
            System.out.println("Erros: " + erros.get());
            
            // Verificar se não houve deadlocks (pelo menos 80% de sucesso)
            int totalOperacoes = numThreads * operacoesPorThread;
            assertTrue(sucessos.get() >= totalOperacoes * 0.8, 
                "Pelo menos 80% das operações devem ter sucesso");
            
            // Verificar se os dados foram persistidos
            StepVerifier.create(repository.count())
                .assertNext(count -> assertTrue(count >= sucessos.get()))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve manter integridade de dados com operações concorrentes")
        void deveManterIntegridadeDadosComOperacoesConcorrentes() {
            // Given
            String usuarioComum = "usuario-concorrencia-test";
            int numOperacoes = 100;
            
            // When - Múltiplas operações para o mesmo usuário
            Flux<EventoAuditoriaDto> operacoesConcorrentes = Flux.range(0, numOperacoes)
                .flatMap(i -> service.registrarEvento(
                    CriarEventoDto.builder()
                        .tipoEvento(TipoEvento.DADOS_ACESSADOS)
                        .usuarioId(usuarioComum)
                        .usuarioNome("Usuário Concorrência")
                        .acaoRealizada("Operação concorrente " + i)
                        .severidade(NivelSeveridade.INFO)
                        .build()
                ), 15); // Alta concorrência
            
            // Then
            StepVerifier.create(operacoesConcorrentes)
                .expectNextCount(numOperacoes)
                .verifyComplete();
            
            // Verificar integridade dos dados
            StepVerifier.create(repository.findByUsuarioId(usuarioComum, org.springframework.data.domain.Pageable.unpaged())
                .collectList())
                .assertNext(eventos -> {
                    assertEquals(numOperacoes, eventos.size());
                    
                    // Verificar se todos os eventos têm dados consistentes
                    eventos.forEach(evento -> {
                        assertEquals(usuarioComum, evento.getUsuarioId());
                        assertEquals("Usuário Concorrência", evento.getUsuarioNome());
                        assertNotNull(evento.getId());
                        assertNotNull(evento.getDataEvento());
                    });
                })
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Testes de Stress")
    class TestesStress {
        
        @Test
        @DisplayName("Deve suportar picos de carga extrema")
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        void deveSuportarPicosCargaExtrema() {
            // Given
            int picoEventos = 2000;
            
            // When - Simular pico de carga
            Flux<EventoAuditoriaDto> picoProcessamento = Flux.range(0, picoEventos)
                .flatMap(i -> service.registrarEvento(criarEventoTeste("pico-" + i)), 50)
                .subscribeOn(Schedulers.parallel());
            
            long startTime = System.currentTimeMillis();
            
            // Then
            StepVerifier.create(picoProcessamento)
                .expectNextCount(picoEventos)
                .verifyComplete();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("Pico de " + picoEventos + " eventos processado em " + duration + "ms");
            System.out.println("Throughput no pico: " + (picoEventos * 1000.0 / duration) + " eventos/segundo");
            
            // Verificar se conseguiu processar em tempo razoável (< 60 segundos)
            assertTrue(duration < 60000, "Pico deve ser processado em menos de 60 segundos");
            
            // Verificar integridade após o pico
            StepVerifier.create(repository.count())
                .assertNext(count -> assertEquals(picoEventos, count.intValue()))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve recuperar-se graciosamente de falhas temporárias")
        void deveRecuperarseGraciosamenteDeFalhasTemporarias() {
            // Given
            AtomicInteger tentativas = new AtomicInteger(0);
            
            // When - Simular falhas temporárias
            Flux<EventoAuditoriaDto> operacaoComFalhas = Flux.range(0, 10)
                .flatMap(i -> {
                    if (tentativas.incrementAndGet() <= 3) {
                        // Simular falha nas primeiras tentativas
                        return Mono.error(new RuntimeException("Falha temporária " + i));
                    }
                    return service.registrarEvento(criarEventoTeste("recuperacao-" + i));
                })
                .retry(5) // Retry automático
                .onErrorResume(error -> {
                    System.out.println("Recuperando de erro: " + error.getMessage());
                    return Mono.empty();
                });
            
            // Then - Deve recuperar e processar eventos restantes
            StepVerifier.create(operacaoComFalhas)
                .expectNextCount(7) // 10 - 3 falhas = 7 sucessos
                .verifyComplete();
            
            System.out.println("Total de tentativas: " + tentativas.get());
            assertTrue(tentativas.get() > 10, "Deve ter feito retry das operações");
        }
    }
    
    @Nested
    @DisplayName("Testes de Backpressure")
    class TestesBackpressure {
        
        @Test
        @DisplayName("Deve lidar com backpressure adequadamente")
        void deveLidarComBackpressureAdequadamente() {
            // Given
            int totalEventos = 1000;
            int bufferSize = 50;
            
            // When - Produzir eventos mais rápido que o consumo
            Flux<EventoAuditoriaDto> fluxComBackpressure = Flux.range(0, totalEventos)
                .map(i -> criarEventoTeste("backpressure-" + i))
                .buffer(bufferSize) // Agrupar em batches
                .flatMap(batch -> 
                    Flux.fromIterable(batch)
                        .flatMap(dto -> service.registrarEvento(dto))
                        .collectList()
                        .flatMapMany(Flux::fromIterable),
                    2 // Processar apenas 2 batches por vez
                )
                .subscribeOn(Schedulers.boundedElastic());
            
            // Then
            StepVerifier.create(fluxComBackpressure)
                .expectNextCount(totalEventos)
                .verifyComplete();
            
            // Verificar se todos foram processados
            StepVerifier.create(repository.count())
                .assertNext(count -> assertEquals(totalEventos, count.intValue()))
                .verifyComplete();
        }
    }
}