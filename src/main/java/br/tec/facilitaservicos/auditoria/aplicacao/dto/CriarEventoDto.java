package br.tec.facilitaservicos.auditoria.aplicacao.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;
import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para criação de um novo evento de auditoria.
 * Utilizado para registrar ações e eventos importantes no sistema,
 * garantindo rastreabilidade e conformidade.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Dados para criação de um novo evento de auditoria")
public class CriarEventoDto {
    @Schema(description = "Tipo do evento de auditoria", example = "LOGIN_SUCESSO")
    @NotNull(message = "Tipo do evento é obrigatório")
    private TipoEvento tipoEvento;
    @Schema(description = "Identificador único do usuário que disparou o evento", example = "user-123")
    @NotBlank(message = "ID do usuário é obrigatório")
    private String usuarioId;
    @Schema(description = "Nome do usuário que disparou o evento", example = "João Silva")
    private String usuarioNome;
    @Schema(description = "ID da sessão do usuário (se aplicável)", example = "sessao-abc-123")
    private String sessaoId;
    @Schema(description = "Endereço IP de origem do evento", example = "192.168.1.1")
    private String ipOrigem;
    @Schema(description = "User-Agent do cliente que disparou o evento", example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
    private String userAgent;
    @Schema(description = "Tipo da entidade afetada pelo evento (ex: USUARIO, PRODUTO)", example = "USUARIO")
    private String entidadeTipo;
    @Schema(description = "ID da entidade afetada pelo evento", example = "user-456")
    private String entidadeId;
    @Schema(description = "Nome da entidade afetada pelo evento", example = "Usuário Teste")
    private String entidadeNome;
    @Schema(description = "Descrição da ação realizada", example = "Login de usuário")
    @NotBlank(message = "Ação realizada é obrigatória")
    private String acaoRealizada;
    @Schema(description = "Estado dos dados antes da ação (JSON)", example = "{\"oldValue\": \"valorAntigo\"}")
    private Map<String, Object> dadosAntes;
    @Schema(description = "Estado dos dados depois da ação (JSON)", example = "{\"newValue\": \"valorNovo\"}")
    private Map<String, Object> dadosDepois;
    @Schema(description = "Metadados adicionais do evento (JSON)", example = "{\"origem\": \"API\", \"versao\": \"1.0\"}")
    private Map<String, Object> metadados;
    @Schema(description = "Nível de severidade do evento", example = "ALTA", allowableValues = {"BAIXA", "MEDIA", "ALTA", "CRITICA"})
    @NotNull(message = "Nível de severidade é obrigatório")
    private NivelSeveridade severidade;
    @Schema(description = "ID de rastreamento distribuído (OpenTelemetry/Zipkin)", example = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
    private String traceId;
    @Schema(description = "ID do span dentro do rastreamento distribuído", example = "f7e6d5c4b3a2b1c0")
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

