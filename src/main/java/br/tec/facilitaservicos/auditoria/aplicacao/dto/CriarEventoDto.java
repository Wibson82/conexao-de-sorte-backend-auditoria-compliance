package br.tec.facilitaservicos.auditoria.aplicacao.dto;

import java.util.Map;

import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;
import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;

public class CriarEventoDto {
    private TipoEvento tipoEvento;
    private String usuarioId;
    private String usuarioNome;
    private String sessaoId;
    private String ipOrigem;
    private String userAgent;
    private String entidadeTipo;
    private String entidadeId;
    private String entidadeNome;
    private String acaoRealizada;
    private Map<String, Object> dadosAntes;
    private Map<String, Object> dadosDepois;
    private Map<String, Object> metadados;
    private NivelSeveridade severidade;
    private String traceId;
    private String spanId;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CriarEventoDto dto = new CriarEventoDto();
        public Builder tipoEvento(TipoEvento v){ dto.tipoEvento=v; return this; }
        public Builder usuarioId(String v){ dto.usuarioId=v; return this; }
        public Builder usuarioNome(String v){ dto.usuarioNome=v; return this; }
        public Builder sessaoId(String v){ dto.sessaoId=v; return this; }
        public Builder ipOrigem(String v){ dto.ipOrigem=v; return this; }
        public Builder userAgent(String v){ dto.userAgent=v; return this; }
        public Builder entidadeTipo(String v){ dto.entidadeTipo=v; return this; }
        public Builder entidadeId(String v){ dto.entidadeId=v; return this; }
        public Builder entidadeNome(String v){ dto.entidadeNome=v; return this; }
        public Builder acaoRealizada(String v){ dto.acaoRealizada=v; return this; }
        public Builder dadosAntes(Map<String,Object> v){ dto.dadosAntes=v; return this; }
        public Builder dadosDepois(Map<String,Object> v){ dto.dadosDepois=v; return this; }
        public Builder metadados(Map<String,Object> v){ dto.metadados=v; return this; }
        public Builder severidade(NivelSeveridade v){ dto.severidade=v; return this; }
        public Builder traceId(String v){ dto.traceId=v; return this; }
        public Builder spanId(String v){ dto.spanId=v; return this; }
        public CriarEventoDto build(){ return dto; }
    }

    // Accessors com nomes simples (estilo record)
    public TipoEvento tipoEvento(){ return tipoEvento; }
    public String usuarioId(){ return usuarioId; }
    public String usuarioNome(){ return usuarioNome; }
    public String sessaoId(){ return sessaoId; }
    public String ipOrigem(){ return ipOrigem; }
    public String userAgent(){ return userAgent; }
    public String entidadeTipo(){ return entidadeTipo; }
    public String entidadeId(){ return entidadeId; }
    public String entidadeNome(){ return entidadeNome; }
    public String acaoRealizada(){ return acaoRealizada; }
    public Map<String,Object> dadosAntes(){ return dadosAntes; }
    public Map<String,Object> dadosDepois(){ return dadosDepois; }
    public Map<String,Object> metadados(){ return metadados; }
    public NivelSeveridade severidade(){ return severidade; }
    public String traceId(){ return traceId; }
    public String spanId(){ return spanId; }
}

