package br.tec.facilitaservicos.auditoria;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.tec.facilitaservicos.auditoria.aplicacao.dto.CriarEventoDto;
import br.tec.facilitaservicos.auditoria.aplicacao.mapper.EventoAuditoriaMapper;
import br.tec.facilitaservicos.auditoria.aplicacao.servico.EventoAuditoriaService;
import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;
import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;
import br.tec.facilitaservicos.auditoria.dominio.enums.StatusEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;
import br.tec.facilitaservicos.auditoria.dominio.repositorio.EventoAuditoriaRepository;
import br.tec.facilitaservicos.auditoria.infraestrutura.cache.AuditoriaCacheService;
import br.tec.facilitaservicos.auditoria.infraestrutura.seguranca.HashIntegridadeService;
import br.tec.facilitaservicos.auditoria.infraestrutura.streaming.EventStreamingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * ============================================================================
 * 🧪 TESTES DO SERVIÇO DE EVENTOS DE AUDITORIA
 * ============================================================================
 * 
 * Testes abrangentes para EventoAuditoriaService cobrindo:
 * 
 * ✅ Criação de eventos com integridade
 * ✅ Consultas e filtros
 * ✅ Compliance e LGPD
 * ✅ Integridade e segurança
 * ✅ Cache e performance
 * ✅ Streaming de eventos
 * ✅ Cenários de erro
 * ✅ Monitoramento e saúde
 * 
 * @author Sistema de Testes R2DBC
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventoAuditoriaService - Testes Unitários")
class EventoAuditoriaServiceTest {

    @Mock
    private EventoAuditoriaRepository repository;
    
    @Mock
    private EventoAuditoriaMapper mapper;
    
    @Mock
    private AuditoriaCacheService cacheService;
    
    @Mock
    private EventStreamingService streamingService;
    
    @Mock
    private HashIntegridadeService hashService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private EventoAuditoriaService service;
    
    private CriarEventoDto criarEventoDto;
    private EventoAuditoriaR2dbc eventoEntity;
    private EventoAuditoriaDto eventoDto;
    private final String eventoId = UUID.randomUUID().toString();
    private final String usuarioId = "user-123";
    private final String hashAnterior = "hash-anterior-123";
    private final String novoHash = "novo-hash-456";
    
    @BeforeEach
    void setUp() {
        // Configurar propriedades via reflection
        ReflectionTestUtils.setField(service, "retencaoPadraoDias", 730);
        ReflectionTestUtils.setField(service, "sistemaNome", "conexao-de-sorte");
        ReflectionTestUtils.setField(service, "sistemaVersao", "1.0.0");
        ReflectionTestUtils.setField(service, "streamingHabilitado", true);
        
        // Criar DTOs de teste
        criarEventoDto = CriarEventoDto.builder()
            .tipoEvento(TipoEvento.LOGIN_SUCESSO)
            .usuarioId(usuarioId)
            .usuarioNome("João Silva")
            .sessaoId("sessao-123")
            .ipOrigem("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .entidadeTipo("USUARIO")
            .entidadeId(usuarioId)
            .entidadeNome("João Silva")
            .acaoRealizada("Login realizado com sucesso")
            .dadosAntes(Map.of("status", "offline"))
            .dadosDepois(Map.of("status", "online"))
            .metadados(Map.of("origem", "web"))
            .severidade(NivelSeveridade.INFO)
            .traceId("trace-123")
            .spanId("span-456")
            .build();
            
        eventoEntity = EventoAuditoriaR2dbc.builder()
            .id(eventoId)
            .tipoEvento(TipoEvento.LOGIN_SUCESSO)
            .usuario(usuarioId, "João Silva")
            .sessao("sessao-123", "192.168.1.1", "Mozilla/5.0")
            .entidade("USUARIO", usuarioId, "João Silva")
            .acao("Login realizado com sucesso")
            .dados("{\"status\":\"offline\"}", "{\"status\":\"online\"}")
            .metadados("{\"origem\":\"web\"}")
            .severidade(NivelSeveridade.INFO)
            .compliance("AUTENTICACAO", false, LocalDateTime.now().plusDays(730))
            .rastreamento("trace-123", "span-456")
            .sistema("conexao-de-sorte", "1.0.0")
            .build();
            
        eventoDto = new EventoAuditoriaDto(
            eventoId,
            "LOGIN_SUCESSO",
            LocalDateTime.now(),
            usuarioId,
            "João Silva",
            "Login realizado com sucesso",
            "USUARIO",
            usuarioId,
            "João Silva",
            "VALIDADO",
            "INFO",
            "192.168.1.1",
            "Mozilla/5.0",
            Map.of("origem", "web"),
            novoHash,
            hashAnterior,
            false,
            "AUTENTICACAO",
            LocalDateTime.now().plusDays(730),
            false,
            LocalDateTime.now()
        );
    }
    
    @Nested
    @DisplayName("Criação de Eventos")
    class CriacaoEventos {
        
        @Test
        @DisplayName("Deve registrar evento com sucesso")
        void deveRegistrarEventoComSucesso() throws Exception {
            // Given
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.just(eventoEntity.toBuilder().hashEvento(hashAnterior).build()));
            when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"status\":\"offline\"}", "{\"status\":\"online\"}", "{\"origem\":\"web\"}");
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.just("success"));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.registrarEvento(criarEventoDto))
                .expectNext(eventoDto)
                .verifyComplete();
                
            verify(repository).findUltimoEventoComHash();
            verify(repository).save(any(EventoAuditoriaR2dbc.class));
            verify(hashService).calcularHashEvento(any());
            verify(streamingService).emitirEvento(any());
            verify(cacheService).cachearEvento(anyString(), any());
        }
        
        @Test
        @DisplayName("Deve registrar primeiro evento sem hash anterior")
        void deveRegistrarPrimeiroEventoSemHashAnterior() throws Exception {
            // Given
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"origem\":\"web\"}");
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.just("success"));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.registrarEvento(criarEventoDto))
                .expectNext(eventoDto)
                .verifyComplete();
                
            verify(repository).findUltimoEventoComHash();
            verify(repository).save(argThat(evento -> 
                evento.getHashAnterior() == null || evento.getHashAnterior().isEmpty()
            ));
        }
        
        @Test
        @DisplayName("Deve falhar ao serializar dados inválidos")
        void deveFalharAoSerializarDadosInvalidos() throws Exception {
            // Given
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(objectMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Erro de serialização") {});
            
            // When & Then
            StepVerifier.create(service.registrarEvento(criarEventoDto))
                .expectError(IllegalArgumentException.class)
                .verify();
        }
        
        @Test
        @DisplayName("Deve registrar evento simples")
        void deveRegistrarEventoSimples() {
            // Given
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.just("success"));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.registrarEventoSimples(
                TipoEvento.LOGIN_SUCESSO,
                usuarioId,
                "João Silva",
                "Login realizado"
            ))
                .expectNext(eventoDto)
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Consultas de Eventos")
    class ConsultasEventos {
        
        @Test
        @DisplayName("Deve buscar evento por ID do cache")
        void deveBuscarEventoPorIdDoCache() {
            // Given
            when(cacheService.buscarEventoCache(eventoId))
                .thenReturn(Mono.just(eventoDto));
            
            // When & Then
            StepVerifier.create(service.buscarEventoPorId(eventoId))
                .expectNext(eventoDto)
                .verifyComplete();
                
            verify(cacheService).buscarEventoCache(eventoId);
            verifyNoInteractions(repository);
        }
        
        @Test
        @DisplayName("Deve buscar evento por ID do repositório quando não está no cache")
        void deveBuscarEventoPorIdDoRepositorio() {
            // Given
            when(cacheService.buscarEventoCache(eventoId))
                .thenReturn(Mono.empty());
            when(repository.findById(eventoId))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(eventoEntity))
                .thenReturn(eventoDto);
            when(cacheService.cachearEvento(eventoId, eventoDto))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.buscarEventoPorId(eventoId))
                .expectNext(eventoDto)
                .verifyComplete();
                
            verify(cacheService).buscarEventoCache(eventoId);
            verify(repository).findById(eventoId);
            verify(cacheService).cachearEvento(eventoId, eventoDto);
        }
        
        @Test
        @DisplayName("Deve buscar eventos por usuário")
        void deveBuscarEventosPorUsuario() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findByUsuarioId(usuarioId, pageable))
                .thenReturn(Flux.just(eventoEntity));
            when(mapper.paraDto(eventoEntity))
                .thenReturn(eventoDto);
            
            // When & Then
            StepVerifier.create(service.buscarEventosUsuario(usuarioId, pageable))
                .expectNext(eventoDto)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve buscar eventos por tipo")
        void deveBuscarEventosPorTipo() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findByTipoEvento(TipoEvento.LOGIN_SUCESSO, pageable))
                .thenReturn(Flux.just(eventoEntity));
            when(mapper.paraDto(eventoEntity))
                .thenReturn(eventoDto);
            
            // When & Then
            StepVerifier.create(service.buscarEventosPorTipo(TipoEvento.LOGIN_SUCESSO, pageable))
                .expectNext(eventoDto)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve buscar eventos por período")
        void deveBuscarEventosPorPeriodo() {
            // Given
            LocalDateTime inicio = LocalDateTime.now().minusDays(7);
            LocalDateTime fim = LocalDateTime.now();
            Pageable pageable = PageRequest.of(0, 10);
            
            when(repository.findByPeriodo(inicio, fim, pageable))
                .thenReturn(Flux.just(eventoEntity));
            when(mapper.paraDto(eventoEntity))
                .thenReturn(eventoDto);
            
            // When & Then
            StepVerifier.create(service.buscarEventosPorPeriodo(inicio, fim, pageable))
                .expectNext(eventoDto)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve buscar timeline de entidade do cache")
        void deveBuscarTimelineEntidadeDoCache() {
            // Given
            String cacheKey = "timeline:USUARIO:" + usuarioId;
            when(cacheService.buscarTimelineCache(cacheKey))
                .thenReturn(Flux.just(eventoDto));
            
            // When & Then
            StepVerifier.create(service.buscarTimelineEntidade("USUARIO", usuarioId))
                .expectNext(eventoDto)
                .verifyComplete();
                
            verify(cacheService).buscarTimelineCache(cacheKey);
            verifyNoInteractions(repository);
        }
        
        @Test
        @DisplayName("Deve buscar timeline de entidade do repositório")
        void deveBuscarTimelineEntidadeDoRepositorio() {
            // Given
            String cacheKey = "timeline:USUARIO:" + usuarioId;
            when(cacheService.buscarTimelineCache(cacheKey))
                .thenReturn(Flux.empty());
            when(repository.getTimelineEntidade("USUARIO", usuarioId))
                .thenReturn(Flux.just(eventoEntity));
            when(mapper.paraDto(eventoEntity))
                .thenReturn(eventoDto);
            when(cacheService.cachearTimeline(eq(cacheKey), anyList()))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.buscarTimelineEntidade("USUARIO", usuarioId))
                .expectNext(eventoDto)
                .verifyComplete();
                
            verify(repository).getTimelineEntidade("USUARIO", usuarioId);
            verify(cacheService).cachearTimeline(eq(cacheKey), anyList());
        }
    }
    
    @Nested
    @DisplayName("Compliance e LGPD")
    class ComplianceLGPD {
        
        @Test
        @DisplayName("Deve buscar dados pessoais de usuário")
        void deveBuscarDadosPessoaisUsuario() {
            // Given
            String cpf = "12345678901";
            when(repository.findDadosPessoaisUsuario(usuarioId, cpf))
                .thenReturn(Flux.just(eventoEntity));
            when(mapper.paraDto(eventoEntity))
                .thenReturn(eventoDto);
            
            // When & Then
            StepVerifier.create(service.buscarDadosPessoaisUsuario(usuarioId, cpf))
                .expectNext(eventoDto)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve anonimizar dados de usuário")
        void deveAnonimizarDadosUsuario() {
            // Given
            EventoAuditoriaR2dbc eventoComDadosPessoais = EventoAuditoriaR2dbc.builder()
                .id(eventoId)
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuario(usuarioId, "João Silva")
                .sessao("sessao-123", "192.168.1.1", "Mozilla/5.0")
                .entidade("USUARIO", usuarioId, "João Silva")
                .acao("Login realizado com sucesso")
                .dados("{\"status\":\"offline\"}", "{\"status\":\"online\"}")
                .metadados("{\"origem\":\"web\"}")
                .severidade(NivelSeveridade.INFO)
                .compliance("AUTENTICACAO", true, LocalDateTime.now().plusDays(730))
                .rastreamento("trace-123", "span-456")
                .sistema("conexao-de-sorte", "1.0.0")
                .build();
                
            when(repository.findDadosPessoaisUsuario(usuarioId, ""))
                .thenReturn(Flux.just(eventoComDadosPessoais));
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoComDadosPessoais));
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.just("success"));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.empty());
            when(cacheService.invalidarCachesUsuario(usuarioId))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.anonimizarDadosUsuario(usuarioId))
                .expectNext(1L)
                .verifyComplete();
                
            verify(repository).findDadosPessoaisUsuario(usuarioId, "");
            verify(repository, times(2)).save(any(EventoAuditoriaR2dbc.class));
            verify(cacheService).invalidarCachesUsuario(usuarioId);
        }
        
        @Test
        @DisplayName("Deve processar eventos expirados")
        void deveProcessarEventosExpirados() {
            // Given
            when(repository.marcarEventosExpirados())
                .thenReturn(Mono.just(5));
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.just("success"));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.processarEventosExpirados())
                .expectNext(5)
                .verifyComplete();
                
            verify(repository).marcarEventosExpirados();
        }
    }
    
    @Nested
    @DisplayName("Integridade e Segurança")
    class IntegridadeSeguranca {
        
        @Test
        @DisplayName("Deve verificar integridade com sucesso")
        void deveVerificarIntegridadeComSucesso() {
            // Given
            when(repository.verificarIntegridadeHash())
                .thenReturn(Mono.just(0L)); // Nenhuma quebra
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.just("success"));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.verificarIntegridade())
                .expectNext(true)
                .verifyComplete();
                
            verify(repository).verificarIntegridadeHash();
        }
        
        @Test
        @DisplayName("Deve detectar quebra de integridade")
        void deveDetectarQuebraIntegridade() {
            // Given
            when(repository.verificarIntegridadeHash())
                .thenReturn(Mono.just(3L)); // 3 quebras detectadas
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.just("success"));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.verificarIntegridade())
                .expectNext(false)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve reprocessar eventos com falha")
        void deveReprocessarEventosComFalha() {
            // Given
            EventoAuditoriaR2dbc eventoComFalha = EventoAuditoriaR2dbc.builder()
                .id(eventoId)
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuario(usuarioId, "João Silva")
                .sessao("sessao-123", "192.168.1.1", "Mozilla/5.0")
                .entidade("USUARIO", usuarioId, "João Silva")
                .acao("Login realizado com sucesso")
                .dados("{\"status\":\"offline\"}", "{\"status\":\"online\"}")
                .metadados("{\"origem\":\"web\"}")
                .severidade(NivelSeveridade.INFO)
                .compliance("AUTENTICACAO", false, LocalDateTime.now().plusDays(730))
                .rastreamento("trace-123", "span-456")
                .sistema("conexao-de-sorte", "1.0.0")
                .build();
            eventoComFalha.setStatusEvento(StatusEvento.FALHA);
                
            when(repository.findByStatusEvento(StatusEvento.FALHA, Pageable.unpaged()))
                .thenReturn(Flux.just(eventoComFalha));
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoComFalha));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.reprocessarEvento(any()))
                .thenReturn(Mono.just("reprocessed"));
            
            // When & Then
            StepVerifier.create(service.reprocessarEventosComFalha())
                .expectNext(1L)
                .verifyComplete();
                
            verify(repository).findByStatusEvento(StatusEvento.FALHA, Pageable.unpaged());
            verify(streamingService).reprocessarEvento(any());
        }
    }
    
    @Nested
    @DisplayName("Monitoramento e Saúde")
    class MonitoramentoSaude {
        
        @Test
        @DisplayName("Deve verificar saúde do serviço - HEALTHY")
        void deveVerificarSaudeServicoHealthy() {
            // Given
            when(repository.countEventosHoje())
                .thenReturn(Mono.just(100L));
            when(repository.countEventosCriticosNaoProcessados())
                .thenReturn(Mono.just(0L));
            when(cacheService.obterEstatisticasCache())
                .thenReturn(Mono.just(Map.of("hits", 95, "misses", 5)));
            
            // When & Then
            StepVerifier.create(service.verificarSaude())
                .assertNext(saude -> {
                    assertEquals(100L, saude.get("eventosHoje"));
                    assertEquals(0L, saude.get("eventosCriticosNaoProcessados"));
                    assertEquals("HEALTHY", saude.get("status"));
                    assertNotNull(saude.get("timestamp"));
                    assertNotNull(saude.get("cacheStats"));
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve verificar saúde do serviço - DEGRADED")
        void deveVerificarSaudeServicoDegraded() {
            // Given
            when(repository.countEventosHoje())
                .thenReturn(Mono.just(50L));
            when(repository.countEventosCriticosNaoProcessados())
                .thenReturn(Mono.just(5L)); // Eventos críticos pendentes
            when(cacheService.obterEstatisticasCache())
                .thenReturn(Mono.just(Map.of("hits", 70, "misses", 30)));
            
            // When & Then
            StepVerifier.create(service.verificarSaude())
                .assertNext(saude -> {
                    assertEquals(50L, saude.get("eventosHoje"));
                    assertEquals(5L, saude.get("eventosCriticosNaoProcessados"));
                    assertEquals("DEGRADED", saude.get("status"));
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve obter resumo executivo")
        void deveObterResumoExecutivo() {
            // Given
            LocalDateTime dataInicio = LocalDateTime.now().minusDays(30);
            Map<String, Object> resumo = Map.of(
                "totalEventos", 1000L,
                "eventosCriticos", 10L,
                "usuariosAtivos", 50L
            );
            
            when(repository.getResumoExecutivo(dataInicio))
                .thenReturn(Mono.just(resumo));
            
            // When & Then
            StepVerifier.create(service.obterResumoExecutivo(dataInicio))
                .expectNext(resumo)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve contar eventos críticos não processados")
        void deveContarEventosCriticosNaoProcessados() {
            // Given
            when(repository.countEventosCriticosNaoProcessados())
                .thenReturn(Mono.just(3L));
            
            // When & Then
            StepVerifier.create(service.contarEventosCriticosNaoProcessados())
                .expectNext(3L)
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Utilitários")
    class Utilitarios {
        
        @Test
        @DisplayName("Deve contar total de eventos")
        void deveContarTotalEventos() {
            // Given
            when(repository.count())
                .thenReturn(Mono.just(1500L));
            
            // When & Then
            StepVerifier.create(service.contarEventos(Map.of()))
                .expectNext(1500L)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve limpar caches")
        void deveLimparCaches() {
            // Given
            when(cacheService.limparTodoCache())
                .thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(service.limparCaches())
                .verifyComplete();
                
            verify(cacheService).limparTodoCache();
        }
    }
    
    @Nested
    @DisplayName("Cenários de Erro")
    class CenariosErro {
        
        @Test
        @DisplayName("Deve lidar com erro no streaming sem falhar o fluxo principal")
        void deveLidarComErroStreamingSemFalharFluxo() throws Exception {
            // Given
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"origem\":\"web\"}");
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.error(new RuntimeException("Erro no Kafka")));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.empty());
            
            // When & Then - O evento deve ser criado mesmo com erro no streaming
            StepVerifier.create(service.registrarEvento(criarEventoDto))
                .expectNext(eventoDto)
                .verifyComplete();
                
            verify(streamingService).emitirEvento(any());
        }
        
        @Test
        @DisplayName("Deve lidar com erro no cache sem falhar o fluxo principal")
        void deveLidarComErroCacheSemFalharFluxo() throws Exception {
            // Given
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.empty());
            when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"origem\":\"web\"}");
            when(hashService.calcularHashEvento(any()))
                .thenReturn(novoHash);
            when(repository.save(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(Mono.just(eventoEntity));
            when(mapper.paraDto(any(EventoAuditoriaR2dbc.class)))
                .thenReturn(eventoDto);
            when(streamingService.emitirEvento(any()))
                .thenReturn(Mono.just("success"));
            when(cacheService.cachearEvento(anyString(), any()))
                .thenReturn(Mono.error(new RuntimeException("Erro no Redis")));
            
            // When & Then - O evento deve ser criado mesmo com erro no cache
            StepVerifier.create(service.registrarEvento(criarEventoDto))
                .expectNext(eventoDto)
                .verifyComplete();
                
            verify(cacheService).cachearEvento(anyString(), any());
        }
        
        @Test
        @DisplayName("Deve falhar quando repositório falha")
        void deveFalharQuandoRepositorioFalha() {
            // Given
            when(repository.findUltimoEventoComHash())
                .thenReturn(Mono.error(new RuntimeException("Erro no banco")));
            
            // When & Then
            StepVerifier.create(service.registrarEvento(criarEventoDto))
                .expectError(RuntimeException.class)
                .verify();
        }
    }
}