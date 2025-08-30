package br.tec.facilitaservicos.auditoria.infraestrutura.streaming;

import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import reactor.core.publisher.Mono;

@Service
public class EventStreamingService {
    public Mono<Void> publicarEvento(EventoAuditoriaDto evento) {
        // Implementação mínima: nenhum side-effect
        return Mono.empty();
    }
}

