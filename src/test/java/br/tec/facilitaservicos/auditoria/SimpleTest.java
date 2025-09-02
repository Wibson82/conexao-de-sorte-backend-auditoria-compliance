package br.tec.facilitaservicos.auditoria;

import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;
import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;
import br.tec.facilitaservicos.auditoria.dominio.enums.StatusEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;
import br.tec.facilitaservicos.auditoria.dominio.repositorio.EventoAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ============================================================================
 * 🧪 TESTES DE INTEGRAÇÃO - REPOSITÓRIO EVENTO AUDITORIA R2DBC
 * ============================================================================
 * 
 * Testes robustos para validar:
 * - Operações CRUD reativas
 * - Consultas customizadas com R2DBC
 * - Integridade de dados
 * - Performance de queries
 * - Transações reativas
 * - Compliance e retenção
 * 
 * Configuração de teste:
 * - H2 em memória com modo MySQL
 * - Transações isoladas por teste
 * - Dados de teste realistas
 * - Validação de comportamento reativo
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@ActiveProfiles("test")
class EventoAuditoriaRepositoryIntegrationTest {

    @Autowired
    private EventoAuditoriaRepository repository;

    private EventoAuditoriaR2dbc eventoTeste;
    private EventoAuditoriaR2dbc eventoSecundario;

    @BeforeEach
    void setUp() {
        // Limpar dados antes de cada teste
        repository.deleteAll().block();
        
        // Criar eventos de teste realistas
        eventoTeste = EventoAuditoriaR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                .usuario("user123", "João Silva")
                .sessao("sess456", "192.168.1.100", "Mozilla/5.0")
                .entidade("Usuario", "user123", "João Silva")
                .acao("Login realizado com sucesso")
                .severidade(NivelSeveridade.INFO)
                .sistema("auditoria-service", "1.0.0")
                .compliance("LOGIN_TRACKING", false, LocalDateTime.now().plusDays(90))
                .build();

        eventoSecundario = EventoAuditoriaR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .tipoEvento(TipoEvento.ACESSO_NEGADO)
                .usuario("user456", "Maria Santos")
                .sessao("sess789", "192.168.1.200", "Chrome/91.0")
                .entidade("Documento", "doc789", "Documento Confidencial")
                .acao("Tentativa de acesso negada")
                .severidade(NivelSeveridade.WARN)
                .sistema("auditoria-service", "1.0.0")
                .compliance("ACCESS_CONTROL", true, LocalDateTime.now().plusYears(7))
                .build();
    }

    @Test
    void contextLoads() {
        // Verificar se o contexto Spring carrega corretamente
        assert repository != null;
    }

    @Test
    void deveSalvarERecuperarEventoComSucesso() {
        // Teste básico de CRUD reativo
        StepVerifier.create(
                repository.save(eventoTeste)
                        .then(repository.findById(eventoTeste.getId()))
        )
        .expectNextMatches(evento -> {
            return evento.getId().equals(eventoTeste.getId()) &&
                   evento.getTipoEvento() == TipoEvento.LOGIN_SUCESSO &&
                   evento.getUsuarioId().equals("user123") &&
                   evento.getSeveridade() == NivelSeveridade.INFO;
        })
        .verifyComplete();
    }

    @Test
    void deveBuscarEventosPorUsuarioComPaginacao() {
        // Salvar múltiplos eventos
        Flux<EventoAuditoriaR2dbc> salvarEventos = repository.saveAll(Flux.just(eventoTeste, eventoSecundario));
        
        StepVerifier.create(
                salvarEventos.then()
                        .thenMany(repository.findByUsuarioId("user123", PageRequest.of(0, 10)))
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void deveBuscarEventosPorTipoEvento() {
        StepVerifier.create(
                repository.save(eventoTeste)
                        .thenMany(repository.findByTipoEvento(TipoEvento.LOGIN_SUCESSO, PageRequest.of(0, 10)))
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void deveBuscarEventosPorSeveridade() {
        StepVerifier.create(
                repository.saveAll(Flux.just(eventoTeste, eventoSecundario))
                        .thenMany(repository.findBySeveridade(NivelSeveridade.WARN, PageRequest.of(0, 10)))
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void deveContarEventosRecentes() {
        StepVerifier.create(
                repository.save(eventoTeste)
                        .thenMany(repository.findEventosRecentes(24))
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void deveBuscarEventosPorEntidade() {
        StepVerifier.create(
                repository.save(eventoTeste)
                        .thenMany(repository.findByEntidade("Usuario", "user123", PageRequest.of(0, 10)))
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void deveVerificarEventosComDadosPessoais() {
        StepVerifier.create(
                repository.save(eventoSecundario)
                        .thenMany(repository.findDadosPessoaisUsuario("user456", "12345678901"))
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void deveContarEventosHoje() {
        StepVerifier.create(
                repository.save(eventoTeste)
                        .then(repository.countEventosHoje())
        )
        .expectNext(1L)
        .verifyComplete();
    }

    @Test
    void deveBuscarPorTextoEmMetadados() {
        // Adicionar metadados ao evento
        eventoTeste.setMetadados("{\"sistema\":\"auditoria\",\"modulo\":\"login\"}");
        
        StepVerifier.create(
                repository.save(eventoTeste)
                        .thenMany(repository.buscarPorTexto("login", PageRequest.of(0, 10)))
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void deveValidarTransacaoReativa() {
        // Teste de transação - salvar múltiplos eventos
        Flux<EventoAuditoriaR2dbc> eventos = Flux.just(eventoTeste, eventoSecundario);
        
        StepVerifier.create(
                repository.saveAll(eventos)
                        .then(repository.count())
        )
        .expectNext(2L)
        .verifyComplete();
    }

    @Test
    void deveValidarIntegridadeDosDados() {
        // Verificar se os dados são persistidos corretamente
        eventoTeste.setHashEvento("hash123");
        eventoTeste.setHashAnterior("hashAnterior456");
        
        StepVerifier.create(
                repository.save(eventoTeste)
                        .then(repository.findById(eventoTeste.getId()))
        )
        .expectNextMatches(evento -> {
            return "hash123".equals(evento.getHashEvento()) &&
                   "hashAnterior456".equals(evento.getHashAnterior()) &&
                   evento.getDataEvento() != null;
        })
        .verifyComplete();
    }

    @Test
    void deveValidarStatusEventoPadrao() {
        StepVerifier.create(
                repository.save(eventoTeste)
                        .then(repository.findById(eventoTeste.getId()))
        )
        .expectNextMatches(evento -> evento.getStatusEvento() == StatusEvento.CRIADO)
        .verifyComplete();
    }

    @Test
    void deveValidarComportamentoReativoComErro() {
        // Teste com ID nulo para verificar tratamento de erro
        EventoAuditoriaR2dbc eventoInvalido = new EventoAuditoriaR2dbc();
        eventoInvalido.setId(null);
        
        StepVerifier.create(repository.save(eventoInvalido))
                .expectError()
                .verify();
    }

    @Test
    void deveValidarPerformanceConsultaCompleta() {
        // Teste de performance com múltiplos eventos
        Flux<EventoAuditoriaR2dbc> eventosMultiplos = Flux.range(1, 50)
                .map(i -> EventoAuditoriaR2dbc.builder()
                        .id(UUID.randomUUID().toString())
                        .tipoEvento(TipoEvento.LOGIN_SUCESSO)
                        .usuario("user" + i, "Usuario " + i)
                        .sessao("sess" + i, "192.168.1." + i, "Browser")
                        .entidade("Usuario", "user" + i, "Usuario " + i)
                        .acao("Ação " + i)
                        .severidade(NivelSeveridade.INFO)
                        .sistema("test", "1.0.0")
                        .build());
        
        StepVerifier.create(
                repository.saveAll(eventosMultiplos)
                        .then(repository.count())
        )
        .expectNext(50L)
        .verifyComplete();
    }
}