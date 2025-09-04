package br.tec.facilitaservicos.privacy.apresentacao.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/v1/privacy")
@Tag(name = "Privacidade", description = "Consentimentos, exportação e exclusão de dados")
public class PrivacidadeController {

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard de privacidade")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> dashboard() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "usuariosComConsentimento", 0,
                "exportacoesPendentes", 0,
                "exclusoesPendentes", 0
        )));
    }

    @GetMapping("/dados-coletados")
    @Operation(summary = "Dados coletados do usuário logado")
    public Mono<ResponseEntity<Map<String, Object>>> dadosColetados() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "dados", Map.of("perfil", true, "notificacoes", true)
        )));
    }

    @PutMapping("/consentimento")
    @Operation(summary = "Atualizar consentimento")
    public Mono<ResponseEntity<Map<String, Object>>> atualizarConsentimento(@RequestBody Map<String, Object> req) {
        return Mono.just(ResponseEntity.ok(Map.of("ok", true)));
    }

    @PostMapping("/exportar-dados")
    @Operation(summary = "Solicitar exportação de dados")
    public Mono<ResponseEntity<Map<String, Object>>> exportarDados() {
        return Mono.just(ResponseEntity.ok(Map.of("solicitacaoId", java.util.UUID.randomUUID().toString())));
    }

    @DeleteMapping("/excluir-dados")
    @Operation(summary = "Solicitar exclusão de dados")
    public Mono<ResponseEntity<Map<String, Object>>> excluirDados() {
        return Mono.just(ResponseEntity.ok(Map.of("status", "em processamento")));
    }

    @GetMapping("/consentimentos")
    @Operation(summary = "Listar consentimentos")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> listarConsentimentos() {
        return Mono.just(ResponseEntity.ok(Map.of("itens", java.util.List.of())));
    }
}

