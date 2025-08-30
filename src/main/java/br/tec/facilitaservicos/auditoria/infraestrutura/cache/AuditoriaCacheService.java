package br.tec.facilitaservicos.auditoria.infraestrutura.cache;

import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import reactor.core.publisher.Mono;

@Service
public class AuditoriaCacheService {
    public Mono<Void> cachearEvento(String id, EventoAuditoriaDto dto) {
        // Implementação mínima sem cache real
        return Mono.empty();
    }
}

