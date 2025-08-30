package br.tec.facilitaservicos.auditoria.aplicacao.mapper;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto;
import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;
import br.tec.facilitaservicos.auditoria.apresentacao.dto.EventoAuditoriaDto.StatusIntegridade;

@Component
public class EventoAuditoriaMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventoAuditoriaDto paraDto(EventoAuditoriaR2dbc e) {
        Map<String,Object> dadosAntes = readJson(e.getDadosAntes());
        Map<String,Object> dadosDepois = readJson(e.getDadosDepois());
        Map<String,Object> metadados = readJson(e.getMetadados());

        StatusIntegridade integridade = e.getStatusEvento() != null && e.getStatusEvento().name().equals("VALIDADO")
            ? StatusIntegridade.VALIDA : StatusIntegridade.PENDENTE_VERIFICACAO;

        return new EventoAuditoriaDto(
            e.getId(),
            e.getTipoEvento() != null ? e.getTipoEvento().name() : "DESCONHECIDO",
            e.getEntidadeNome(),
            e.getEntidadeId(),
            e.getUsuarioId(),
            e.getUsuarioNome(),
            e.getSessaoId(),
            null, // correlationId
            e.getTraceId(),
            dadosAntes,
            dadosDepois,
            metadados,
            e.getHashEvento(),
            e.getHashAnterior(),
            e.getAssinaturaDigital(),
            integridade,
            e.getDataEvento(),
            e.getDataProcessamento() != null ? e.getDataProcessamento() : LocalDateTime.now(),
            e.getOrigemSistema(),
            e.getVersaoSistema(),
            null // contextoSeguranca
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

