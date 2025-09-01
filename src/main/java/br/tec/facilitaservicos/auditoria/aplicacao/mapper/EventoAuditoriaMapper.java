package br.tec.facilitaservicos.auditoria.aplicacao.mapper;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;

@Component
public class EventoAuditoriaMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventoAuditoriaDto paraDto(EventoAuditoriaR2dbc e) {
        Map<String,Object> metadados = readJson(e.getMetadados());

        return new EventoAuditoriaDto(
            e.getId(),
            e.getTipoEvento() != null ? e.getTipoEvento().name() : "DESCONHECIDO",
            e.getDataEvento(),
            e.getUsuarioId(),
            e.getUsuarioNome(),
            e.getAcaoRealizada(),
            e.getEntidadeTipo(),
            e.getEntidadeId(),
            e.getEntidadeNome(),
            e.getStatusEvento() != null ? e.getStatusEvento().name() : "CRIADO",
            e.getSeveridade() != null ? e.getSeveridade().name() : "INFO",
            e.getIpOrigem(),
            e.getUserAgent(),
            metadados,
            e.getHashEvento(),
            e.getHashAnterior(),
            e.isDadosPessoais(),
            e.getCategoriaCompliance(),
            e.getRetencaoAte(),
            e.isAnonimizado(),
            e.getDataProcessamento() != null ? e.getDataProcessamento() : LocalDateTime.now()
        );
    }

    private Map<String,Object> readJson(String json) {
        try {
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, new TypeReference<Map<String,Object>>(){});
        } catch (Exception ex) {
            return null;
        }
    }
}

