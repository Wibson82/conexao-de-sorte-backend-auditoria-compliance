package br.tec.facilitaservicos.privacy.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controlador LGPD - Privacy/Consentimentos.
 */
@RestController
@RequestMapping("/v1/privacy")
public class PrivacyController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('SCOPE_privacy.read')")
    public Mono<Map<String, Object>> dashboard() {
        return Mono.just(Map.of("status", "ok", "dashboard", "privacy"));
    }

    @GetMapping("/dados-coletados")
    @PreAuthorize("hasAuthority('SCOPE_privacy.read')")
    public Mono<Map<String, Object>> dadosColetados() {
        return Mono.just(Map.of("dados", "collected_data_stub"));
    }

    @PutMapping("/consentimento")
    @PreAuthorize("hasAuthority('SCOPE_privacy.write')")
    public Mono<Map<String, Object>> atualizarConsentimento(@RequestBody Map<String, Object> request) {
        return Mono.just(Map.of("status", "consentimento atualizado", "request", request));
    }

    @PostMapping("/exportar-dados")
    @PreAuthorize("hasAuthority('SCOPE_privacy.write')")
    public Mono<Map<String, Object>> exportarDados() {
        return Mono.just(Map.of("status", "export initiated"));
    }

    @DeleteMapping("/excluir-dados")
    @PreAuthorize("hasAuthority('SCOPE_privacy.write')")
    public Mono<Map<String, Object>> excluirDados() {
        return Mono.just(Map.of("status", "deletion initiated"));
    }
}