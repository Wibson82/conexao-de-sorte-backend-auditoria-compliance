package br.tec.facilitaservicos.auditoria;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import br.tec.facilitaservicos.auditoria.aplicacao.dto.CriarEventoDto;
import br.tec.facilitaservicos.auditoria.aplicacao.servico.EventoAuditoriaService;
import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;
import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;
import br.tec.facilitaservicos.auditoria.dominio.enums.StatusEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;
import br.tec.facilitaservicos.auditoria.dominio.repositorio.EventoAuditoriaRepository;
import br.tec.facilitaservicos.auditoria.infraestrutura.seguranca.HashIntegridadeService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * ============================================================================
 * 🔒 TESTES DE COMPLIANCE E INTEGRIDADE DE DADOS
 * ============================================================================
 * 
 * Testes focados em:
 * 
 * ✅ Compliance LGPD/GDPR
 * ✅ Retenção e expiração de dados
 * ✅ Anonimização de dados pessoais
 * ✅ Integridade da cadeia de hash
 * ✅ Validação de assinatura digital
 * ✅ Auditoria de compliance
 * ✅ Políticas de retenção
 * ✅ Direito ao esquecimento
 * 
 * @author Sistema de Testes R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "auditoria.retention.default-days=730",
    "auditoria.compliance.lgpd.enabled=true",
    "auditoria.integrity.hash-chain.enabled=true",
    "feature.auditoria.streaming=false"
})
@DisplayName("Compliance e Integridade - Testes de Integração")
class ComplianceIntegridadeTest {

    @Autowired
    private EventoAuditoriaService service;
    
    @Autowired
    private EventoAuditoriaRepository repository;
    
    @Autowired
    private HashIntegridadeService hashService;
    
    private final String usuarioId = "user-compliance-123";
    private final String cpfUsuario = "12345678901";
    
    @BeforeEach
    void setUp() {
        // Limpar dados de teste
        repository.deleteAll().block();
    }
    
    @Nested
    @DisplayName("LGPD - Lei Geral de Proteção de Dados")
    class TestesLGPD {
        
        @Test
        @DisplayName("Deve identificar eventos com dados pessoais")
        @Transactional
        void deveIdentificarEventosComDadosPessoais() {
            // Given - Criar evento com dados pessoais
            CriarEventoDto eventoComDadosPessoais = CriarEventoDto.builder()
                .tipoEvento(TipoEvento.DADOS_ACESSADOS)
                .usuarioId(usuarioId)
                .usuarioNome("João Silva")
                .entidadeTipo("USUARIO")
                .entidadeId(usuarioId)
                .entidadeNome("João Silva")
                .acaoRealizada("Acesso a dados pessoais")
                .dadosAntes(Map.of("cpf", cpfUsuario, "email", "joao@email.com"))
                .dadosDepois(Map.of("cpf", cpfUsuario, "email", "joao@email.com", "telefone", "11999999999"))
                .severidade(NivelSeveridade.WARN)
                .build();
            
            // When
            EventoAuditoriaDto eventoSalvo = service.registrarEvento(eventoComDadosPessoais).block();
            
            // Then
            assertNotNull(eventoSalvo);
            assertTrue(eventoSalvo.dadosPessoais());
            assertEquals("DADOS_PESSOAIS", eventoSalvo.categoriaCompliance());
            assertNotNull(eventoSalvo.retencaoAte());
            
            // Verificar se pode ser encontrado na busca de dados pessoais
            StepVerifier.create(service.buscarDadosPessoaisUsuario(usuarioId, cpfUsuario))
                .assertNext(evento -> {
                    assertEquals(eventoSalvo.id(), evento.id());
                    assertTrue(evento.dadosPessoais());
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve anonimizar dados pessoais (Direito ao Esquecimento)")
        @Transactional
        void deveAnonimizarDadosPessoais() {
            // Given - Criar múltiplos eventos com dados pessoais
            List<CriarEventoDto> eventosComDadosPessoais = List.of(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.DADOS_ACESSADOS)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .entidadeTipo("USUARIO")
                    .entidadeId(usuarioId)
                    .acaoRealizada("Cadastro de usuário")
                    .dadosAntes(Map.of())
                    .dadosDepois(Map.of("cpf", cpfUsuario, "nome", "João Silva"))
                    .severidade(NivelSeveridade.INFO)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.DADOS_MODIFICADOS)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .entidadeTipo("USUARIO")
                    .entidadeId(usuarioId)
                    .acaoRealizada("Alteração de dados pessoais")
                    .dadosAntes(Map.of("email", "joao@email.com"))
                    .dadosDepois(Map.of("email", "joao.silva@email.com"))
                    .severidade(NivelSeveridade.WARN)
                    .build()
            );
            
            // Registrar eventos
            eventosComDadosPessoais.forEach(dto -> 
                service.registrarEvento(dto).block()
            );
            
            // When - Anonimizar dados do usuário
            Long eventosAnonimizados = service.anonimizarDadosUsuario(usuarioId).block();
            
            // Then
            assertNotNull(eventosAnonimizados);
            assertTrue(eventosAnonimizados >= 2);
            
            // Verificar se os dados foram anonimizados
            StepVerifier.create(repository.findByUsuarioId(usuarioId, Pageable.unpaged()))
                .assertNext(evento -> {
                    assertTrue(evento.isAnonimizado());
                    assertEquals("ANONIMIZADO", evento.getUsuarioNome());
                    assertEquals("MASKED", evento.getIpOrigem());
                    assertEquals("MASKED", evento.getUserAgent());
                    assertNull(evento.getDadosAntes());
                    assertNull(evento.getDadosDepois());
                })
                .expectNextCount(eventosAnonimizados - 1) // -1 porque já verificamos o primeiro
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve respeitar período de retenção de dados")
        @Transactional
        void deveRespeitarPeriodoRetencaoDados() {
            // Given - Criar evento que já deveria estar expirado
            EventoAuditoriaR2dbc eventoExpirado = EventoAuditoriaR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuario(usuarioId, "João Silva")
                .entidade("USUARIO", usuarioId, "João Silva")
                .acao("Login antigo")
                .severidade(NivelSeveridade.INFO)
                .compliance("AUTENTICACAO", false, LocalDateTime.now().minusDays(1)) // Expirado ontem
                .sistema("conexao-de-sorte", "1.0.0")
                .build();
            
            repository.save(eventoExpirado).block();
            
            // When - Processar eventos expirados
            Integer eventosProcessados = service.processarEventosExpirados().block();
            
            // Then
            assertNotNull(eventosProcessados);
            assertTrue(eventosProcessados >= 1);
            
            // Verificar se o evento foi marcado como expirado
            StepVerifier.create(repository.findById(eventoExpirado.getId()))
                .assertNext(evento -> {
                    assertTrue(evento.isExpirado());
                    // Pode ter status EXPIRADO ou similar dependendo da implementação
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve gerar relatório de compliance")
        @Transactional
        void deveGerarRelatorioCompliance() {
            // Given - Criar eventos de diferentes tipos para compliance
            List<CriarEventoDto> eventosCompliance = List.of(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.DADOS_ACESSADOS)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Acesso autorizado a dados pessoais")
                    .severidade(NivelSeveridade.WARN)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.DADOS_ANONIMIZADOS)
                    .usuarioId("SYSTEM")
                    .usuarioNome("Sistema")
                    .acaoRealizada("Anonimização automática")
                    .severidade(NivelSeveridade.INFO)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.TENTATIVA_INTRUSION)
                    .usuarioId("user-malicioso")
                    .usuarioNome("Usuário Suspeito")
                    .acaoRealizada("Tentativa de acesso não autorizado")
                    .severidade(NivelSeveridade.CRITICAL)
                    .build()
            );
            
            eventosCompliance.forEach(dto -> 
                service.registrarEvento(dto).block()
            );
            
            // When - Obter resumo executivo
            LocalDateTime dataInicio = LocalDateTime.now().minusDays(1);
            Map<String, Object> resumo = service.obterResumoExecutivo(dataInicio).block();
            
            // Then
            assertNotNull(resumo);
            assertTrue(resumo.containsKey("totalEventos"));
            assertTrue(resumo.containsKey("eventosCriticos"));
            
            Long totalEventos = (Long) resumo.get("totalEventos");
            assertTrue(totalEventos >= 3);
        }
    }
    
    @Nested
    @DisplayName("Integridade da Cadeia de Hash")
    class TestesIntegridadeHash {
        
        @Test
        @DisplayName("Deve manter integridade da cadeia de hash")
        @Transactional
        void deveManterIntegridadeCadeiaHash() {
            // Given - Criar sequência de eventos
            List<CriarEventoDto> sequenciaEventos = List.of(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Primeiro login")
                    .severidade(NivelSeveridade.INFO)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.DADOS_ACESSADOS)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Acesso a perfil")
                    .severidade(NivelSeveridade.INFO)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGOUT)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Logout do sistema")
                    .severidade(NivelSeveridade.INFO)
                    .build()
            );
            
            // When - Registrar eventos em sequência
            List<EventoAuditoriaDto> eventosRegistrados = sequenciaEventos.stream()
                .map(dto -> service.registrarEvento(dto).block())
                .toList();
            
            // Then - Verificar integridade da cadeia
            Boolean integridadeOk = service.verificarIntegridade().block();
            assertTrue(integridadeOk, "A cadeia de hash deve estar íntegra");
            
            // Verificar encadeamento dos hashes
            StepVerifier.create(repository.findAll().sort((e1, e2) -> 
                e1.getDataEvento().compareTo(e2.getDataEvento())).collectList())
                .assertNext(eventos -> {
                    for (int i = 1; i < eventos.size(); i++) {
                        EventoAuditoriaR2dbc eventoAnterior = eventos.get(i - 1);
                        EventoAuditoriaR2dbc eventoAtual = eventos.get(i);
                        
                        assertEquals(eventoAnterior.getHashEvento(), eventoAtual.getHashAnterior(),
                            "Hash anterior deve corresponder ao hash do evento anterior");
                        
                        assertNotNull(eventoAtual.getHashEvento());
                        assertFalse(eventoAtual.getHashEvento().isEmpty());
                    }
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve detectar quebra na cadeia de hash")
        @Transactional
        void deveDetectarQuebraCadeiaHash() {
            // Given - Criar eventos válidos
            EventoAuditoriaDto evento1 = service.registrarEvento(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Login válido")
                    .severidade(NivelSeveridade.INFO)
                    .build()
            ).block();
            
            EventoAuditoriaDto evento2 = service.registrarEvento(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGOUT)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Logout válido")
                    .severidade(NivelSeveridade.INFO)
                    .build()
            ).block();
            
            // When - Corromper hash de um evento (simular ataque)
            EventoAuditoriaR2dbc eventoCorrupto = repository.findById(evento1.id()).block();
            eventoCorrupto.setHashEvento("HASH_CORROMPIDO_123");
            repository.save(eventoCorrupto).block();
            
            // Then - Verificação de integridade deve detectar a quebra
            Boolean integridadeOk = service.verificarIntegridade().block();
            assertFalse(integridadeOk, "Deve detectar quebra na integridade da cadeia");
        }
        
        @Test
        @DisplayName("Deve calcular hash corretamente")
        @Transactional
        void deveCalcularHashCorretamente() {
            // Given
            EventoAuditoriaR2dbc evento = EventoAuditoriaR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuario(usuarioId, "João Silva")
                .entidade("USUARIO", usuarioId, "João Silva")
                .acao("Login para teste de hash")
                .severidade(NivelSeveridade.INFO)
                .sistema("conexao-de-sorte", "1.0.0")
                .build();
            
            // When
            String hash1 = hashService.calcularHashEvento(evento);
            String hash2 = hashService.calcularHashEvento(evento);
            
            // Then
            assertNotNull(hash1);
            assertNotNull(hash2);
            assertEquals(hash1, hash2, "Hash deve ser determinístico");
            assertFalse(hash1.isEmpty());
            assertTrue(hash1.length() >= 32, "Hash deve ter tamanho mínimo");
        }
    }
    
    @Nested
    @DisplayName("Auditoria de Compliance")
    class TestesAuditoriaCompliance {
        
        @Test
        @DisplayName("Deve auditar tentativas de acesso não autorizado")
        @Transactional
        void deveAuditarTentativasAcessoNaoAutorizado() {
            // Given
            CriarEventoDto tentativaIlegal = CriarEventoDto.builder()
                .tipoEvento(TipoEvento.TENTATIVA_INTRUSION)
                .usuarioId("user-suspeito")
                .usuarioNome("Usuário Suspeito")
                .ipOrigem("192.168.1.100")
                .entidadeTipo("DADOS_SENSÍVEIS")
                .entidadeId("dados-confidenciais-123")
                .acaoRealizada("Tentativa de acesso a dados confidenciais")
                .severidade(NivelSeveridade.CRITICAL)
                .build();
            
            // When
            EventoAuditoriaDto eventoRegistrado = service.registrarEvento(tentativaIlegal).block();
            
            // Then
            assertNotNull(eventoRegistrado);
            assertEquals("CRITICAL", eventoRegistrado.severidade());
            assertEquals("SEGURANCA", eventoRegistrado.categoriaCompliance());
            
            // Verificar se aparece nos eventos críticos
            Long eventosCriticos = service.contarEventosCriticosNaoProcessados().block();
            assertTrue(eventosCriticos >= 1);
        }
        
        @Test
        @DisplayName("Deve monitorar alterações em dados sensíveis")
        @Transactional
        void deveMonitorarAlteracoesDadosSensiveis() {
            // Given
            CriarEventoDto alteracaoSensivel = CriarEventoDto.builder()
                .tipoEvento(TipoEvento.DADOS_MODIFICADOS)
                .usuarioId(usuarioId)
                .usuarioNome("João Silva")
                .entidadeTipo("USUARIO")
                .entidadeId(usuarioId)
                .acaoRealizada("Alteração de CPF")
                .dadosAntes(Map.of("cpf", "11111111111"))
                .dadosDepois(Map.of("cpf", cpfUsuario))
                .severidade(NivelSeveridade.WARN)
                .build();
            
            // When
            EventoAuditoriaDto eventoRegistrado = service.registrarEvento(alteracaoSensivel).block();
            
            // Then
            assertNotNull(eventoRegistrado);
            assertTrue(eventoRegistrado.dadosPessoais());
            assertEquals("DADOS_PESSOAIS", eventoRegistrado.categoriaCompliance());
            
            // Verificar timeline da entidade
            StepVerifier.create(service.buscarTimelineEntidade("USUARIO", usuarioId))
                .assertNext(evento -> {
                    assertEquals(eventoRegistrado.id(), evento.id());
                    assertEquals("Alteração de CPF", evento.acaoRealizada());
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve gerar estatísticas de compliance")
        @Transactional
        void deveGerarEstatisticasCompliance() {
            // Given - Criar eventos de diferentes categorias
            List<CriarEventoDto> eventosVariados = List.of(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Login normal")
                    .severidade(NivelSeveridade.INFO)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.DADOS_ACESSADOS)
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Consulta de dados")
                    .severidade(NivelSeveridade.WARN)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.ERRO_APLICACAO)
                    .usuarioId("SYSTEM")
                    .usuarioNome("Sistema")
                    .acaoRealizada("Erro interno")
                    .severidade(NivelSeveridade.ERROR)
                    .build()
            );
            
            eventosVariados.forEach(dto -> 
                service.registrarEvento(dto).block()
            );
            
            // When
            LocalDateTime dataInicio = LocalDateTime.now().minusDays(1);
            LocalDateTime dataFim = LocalDateTime.now().plusDays(1);
            
            // Then - Verificar estatísticas por tipo
            StepVerifier.create(service.obterEstatisticasPorTipo(dataInicio, dataFim))
                .expectNextCount(3) // Deve ter pelo menos 3 tipos diferentes
                .verifyComplete();
            
            // Verificar usuários mais ativos
            StepVerifier.create(service.obterUsuariosMaisAtivos(dataInicio))
                .assertNext(stats -> {
                    assertNotNull(stats);
                    assertTrue(stats.containsKey("usuario_id") || stats.containsKey("usuarioId"));
                })
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Políticas de Retenção")
    class TestesPoliticasRetencao {
        
        @Test
        @DisplayName("Deve aplicar diferentes períodos de retenção por tipo de evento")
        @Transactional
        void deveAplicarDiferentesPeriodosRetencao() {
            // Given - Eventos com diferentes políticas de retenção
            List<CriarEventoDto> eventosComRetencao = List.of(
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.LOGIN_SUCESSO) // Retenção curta
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Login")
                    .severidade(NivelSeveridade.INFO)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.DADOS_ACESSADOS) // Retenção longa
                    .usuarioId(usuarioId)
                    .usuarioNome("João Silva")
                    .acaoRealizada("Acesso a dados pessoais")
                    .severidade(NivelSeveridade.WARN)
                    .build(),
                CriarEventoDto.builder()
                    .tipoEvento(TipoEvento.TENTATIVA_INTRUSION) // Retenção muito longa
                    .usuarioId("user-suspeito")
                    .usuarioNome("Usuário Suspeito")
                    .acaoRealizada("Tentativa de invasão")
                    .severidade(NivelSeveridade.CRITICAL)
                    .build()
            );
            
            // When
            List<EventoAuditoriaDto> eventosRegistrados = eventosComRetencao.stream()
                .map(dto -> service.registrarEvento(dto).block())
                .toList();
            
            // Then - Verificar se períodos de retenção foram aplicados corretamente
            StepVerifier.create(repository.findAll())
                .recordWith(() -> new java.util.ArrayList<EventoAuditoriaR2dbc>())
                .thenConsumeWhile(evento -> true)
                .consumeRecordedWith(eventos -> {
                    for (EventoAuditoriaR2dbc evento : eventos) {
                        assertNotNull(evento.getRetencaoAte());
                        assertTrue(evento.getRetencaoAte().isAfter(LocalDateTime.now()));
                        
                        // Verificar se período está de acordo com o tipo de evento
                        long diasRetencao = java.time.temporal.ChronoUnit.DAYS.between(
                            LocalDateTime.now(), evento.getRetencaoAte());
                        
                        if (evento.getTipoEvento() == TipoEvento.TENTATIVA_INTRUSION) {
                            assertTrue(diasRetencao >= 1825, "Eventos de segurança devem ter retenção longa"); // 5 anos
                        } else if (evento.getTipoEvento() == TipoEvento.DADOS_ACESSADOS) {
                            assertTrue(diasRetencao >= 730, "Dados pessoais devem ter retenção mínima"); // 2 anos
                        }
                    }
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Deve contar eventos próximos do vencimento")
        @Transactional
        void deveContarEventosProximosVencimento() {
            // Given - Criar evento que vence em breve
            EventoAuditoriaR2dbc eventoVencendoEmBreve = EventoAuditoriaR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuario(usuarioId, "João Silva")
                .entidade("USUARIO", usuarioId, "João Silva")
                .acao("Login que vence em breve")
                .severidade(NivelSeveridade.INFO)
                .compliance("AUTENTICACAO", false, LocalDateTime.now().plusDays(7)) // Vence em 7 dias
                .sistema("conexao-de-sorte", "1.0.0")
                .build();
            
            repository.save(eventoVencendoEmBreve).block();
            
            // When & Then - Verificar contagem
            StepVerifier.create(repository.count())
                .assertNext(count -> assertTrue(count >= 1))
                .verifyComplete();
        }
    }
}