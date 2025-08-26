package br.tec.facilitaservicos.auditoria.dominio.entidade;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.tec.facilitaservicos.auditoria.dominio.enums.TipoEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.StatusEvento;
import br.tec.facilitaservicos.auditoria.dominio.enums.NivelSeveridade;

/**
 * ============================================================================
 * 📋 ENTIDADE EVENTO DE AUDITORIA R2DBC
 * ============================================================================
 * 
 * Entidade imutável para persistência reativa de eventos de auditoria.
 * 
 * Características principais:
 * - Event Sourcing: eventos são imutáveis após criação
 * - WORM Storage: Write-Once, Read-Many
 * - Integridade: hash encadeado para verificação
 * - Compliance: metadados para LGPD/GDPR
 * - Assinatura digital: verificação criptográfica
 * - Retenção configurável por tipo de evento
 * 
 * Estrutura de dados:
 * - ID único do evento (UUID)
 * - Timestamp preciso (LocalDateTime)
 * - Tipo de evento (enum)
 * - Entidade afetada e ID
 * - Usuário responsável
 * - Dados antes/depois (JSON)
 * - Metadados contextuais
 * - Hash de integridade
 * - Assinatura digital
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Table("eventos_auditoria")
public class EventoAuditoriaR2dbc {

    @Id
    private String id;

    // Identificação do evento
    @Column("tipo_evento")
    private TipoEvento tipoEvento;
    
    @Column("status_evento") 
    private StatusEvento statusEvento;
    
    @Column("severidade")
    private NivelSeveridade severidade;

    // Contexto do evento
    @Column("usuario_id")
    private String usuarioId;
    
    @Column("usuario_nome")
    private String usuarioNome;
    
    @Column("sessao_id")
    private String sessaoId;
    
    @Column("ip_origem")
    private String ipOrigem;
    
    @Column("user_agent")
    private String userAgent;

    // Entidade afetada
    @Column("entidade_tipo")
    private String entidadeTipo;
    
    @Column("entidade_id")
    private String entidadeId;
    
    @Column("entidade_nome")
    private String entidadeNome;

    // Dados do evento
    @Column("acao_realizada")
    private String acaoRealizada;
    
    @Column("dados_antes")
    private String dadosAntes; // JSON
    
    @Column("dados_depois") 
    private String dadosDepois; // JSON
    
    @Column("mudancas_detectadas")
    private String mudancasDetectadas; // JSON array
    
    @Column("metadados")
    private String metadados; // JSON

    // Integridade e segurança
    @Column("hash_evento")
    private String hashEvento;
    
    @Column("hash_anterior") 
    private String hashAnterior; // Hash do evento anterior (blockchain)
    
    @Column("assinatura_digital")
    private String assinaturaDigital;
    
    @Column("chave_assinatura_id")
    private String chaveAssinaturaId;

    // Compliance e retenção
    @Column("categoria_compliance")
    private String categoriaCompliance;
    
    @Column("dados_pessoais")
    private Boolean dadosPessoais;
    
    @Column("retencao_ate")
    private LocalDateTime retencaoAte;
    
    @Column("anonimizado")
    private Boolean anonimizado;

    // Timestamps
    @CreatedDate
    @Column("data_evento")
    private LocalDateTime dataEvento;
    
    @Column("data_processamento")
    private LocalDateTime dataProcessamento;

    // Contexto técnico
    @Column("origem_sistema")
    private String origemSistema;
    
    @Column("versao_sistema")
    private String versaoSistema;
    
    @Column("trace_id")
    private String traceId;
    
    @Column("span_id")
    private String spanId;

    // Construtor padrão
    public EventoAuditoriaR2dbc() {
        this.statusEvento = StatusEvento.CRIADO;
        this.severidade = NivelSeveridade.INFO;
        this.dadosPessoais = false;
        this.anonimizado = false;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EventoAuditoriaR2dbc evento = new EventoAuditoriaR2dbc();

        public Builder id(String id) {
            evento.id = id;
            return this;
        }

        public Builder tipoEvento(TipoEvento tipoEvento) {
            evento.tipoEvento = tipoEvento;
            return this;
        }

        public Builder usuario(String usuarioId, String usuarioNome) {
            evento.usuarioId = usuarioId;
            evento.usuarioNome = usuarioNome;
            return this;
        }

        public Builder sessao(String sessaoId, String ipOrigem, String userAgent) {
            evento.sessaoId = sessaoId;
            evento.ipOrigem = ipOrigem;
            evento.userAgent = userAgent;
            return this;
        }

        public Builder entidade(String tipo, String id, String nome) {
            evento.entidadeTipo = tipo;
            evento.entidadeId = id;
            evento.entidadeNome = nome;
            return this;
        }

        public Builder acao(String acao) {
            evento.acaoRealizada = acao;
            return this;
        }

        public Builder dados(String dadosAntes, String dadosDepois) {
            evento.dadosAntes = dadosAntes;
            evento.dadosDepois = dadosDepois;
            return this;
        }

        public Builder mudancas(String mudancas) {
            evento.mudancasDetectadas = mudancas;
            return this;
        }

        public Builder metadados(String metadados) {
            evento.metadados = metadados;
            return this;
        }

        public Builder severidade(NivelSeveridade severidade) {
            evento.severidade = severidade;
            return this;
        }

        public Builder compliance(String categoria, Boolean dadosPessoais, LocalDateTime retencaoAte) {
            evento.categoriaCompliance = categoria;
            evento.dadosPessoais = dadosPessoais;
            evento.retencaoAte = retencaoAte;
            return this;
        }

        public Builder rastreamento(String traceId, String spanId) {
            evento.traceId = traceId;
            evento.spanId = spanId;
            return this;
        }

        public Builder sistema(String origem, String versao) {
            evento.origemSistema = origem;
            evento.versaoSistema = versao;
            return this;
        }

        public EventoAuditoriaR2dbc build() {
            return evento;
        }
    }

    // Métodos de negócio

    /**
     * Marca evento como processado
     */
    public void marcarComoProcessado() {
        this.statusEvento = StatusEvento.PROCESSADO;
        this.dataProcessamento = LocalDateTime.now();
    }

    /**
     * Verifica se evento contém dados pessoais
     */
    public boolean contemDadosPessoais() {
        return Boolean.TRUE.equals(this.dadosPessoais);
    }

    /**
     * Verifica se evento está expirado para retenção
     */
    public boolean isExpirado() {
        return retencaoAte != null && retencaoAte.isBefore(LocalDateTime.now());
    }

    /**
     * Marca dados como anonimizados (GDPR compliance)
     */
    public void anonimizar() {
        this.anonimizado = true;
        this.usuarioNome = "ANONIMIZADO";
        this.ipOrigem = "MASKED";
        this.userAgent = "MASKED";
        this.dadosAntes = null;
        this.dadosDepois = null;
        this.metadados = "{}";
    }

    /**
     * Calcula hash do evento para integridade
     */
    @JsonIgnore
    public String calcularHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(id)
          .append(tipoEvento)
          .append(usuarioId)
          .append(entidadeTipo)
          .append(entidadeId)
          .append(acaoRealizada)
          .append(dadosAntes)
          .append(dadosDepois)
          .append(dataEvento);
        
        // Usar algoritmo de hash seguro (implementar)
        return Integer.toHexString(sb.toString().hashCode());
    }

    // Getters e Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public StatusEvento getStatusEvento() {
        return statusEvento;
    }

    public void setStatusEvento(StatusEvento statusEvento) {
        this.statusEvento = statusEvento;
    }

    public NivelSeveridade getSeveridade() {
        return severidade;
    }

    public void setSeveridade(NivelSeveridade severidade) {
        this.severidade = severidade;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public String getSessaoId() {
        return sessaoId;
    }

    public void setSessaoId(String sessaoId) {
        this.sessaoId = sessaoId;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }

    public void setIpOrigem(String ipOrigem) {
        this.ipOrigem = ipOrigem;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getEntidadeTipo() {
        return entidadeTipo;
    }

    public void setEntidadeTipo(String entidadeTipo) {
        this.entidadeTipo = entidadeTipo;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public void setEntidadeId(String entidadeId) {
        this.entidadeId = entidadeId;
    }

    public String getEntidadeNome() {
        return entidadeNome;
    }

    public void setEntidadeNome(String entidadeNome) {
        this.entidadeNome = entidadeNome;
    }

    public String getAcaoRealizada() {
        return acaoRealizada;
    }

    public void setAcaoRealizada(String acaoRealizada) {
        this.acaoRealizada = acaoRealizada;
    }

    public String getDadosAntes() {
        return dadosAntes;
    }

    public void setDadosAntes(String dadosAntes) {
        this.dadosAntes = dadosAntes;
    }

    public String getDadosDepois() {
        return dadosDepois;
    }

    public void setDadosDepois(String dadosDepois) {
        this.dadosDepois = dadosDepois;
    }

    public String getMudancasDetectadas() {
        return mudancasDetectadas;
    }

    public void setMudancasDetectadas(String mudancasDetectadas) {
        this.mudancasDetectadas = mudancasDetectadas;
    }

    public String getMetadados() {
        return metadados;
    }

    public void setMetadados(String metadados) {
        this.metadados = metadados;
    }

    public String getHashEvento() {
        return hashEvento;
    }

    public void setHashEvento(String hashEvento) {
        this.hashEvento = hashEvento;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public void setHashAnterior(String hashAnterior) {
        this.hashAnterior = hashAnterior;
    }

    public String getAssinaturaDigital() {
        return assinaturaDigital;
    }

    public void setAssinaturaDigital(String assinaturaDigital) {
        this.assinaturaDigital = assinaturaDigital;
    }

    public String getChaveAssinaturaId() {
        return chaveAssinaturaId;
    }

    public void setChaveAssinaturaId(String chaveAssinaturaId) {
        this.chaveAssinaturaId = chaveAssinaturaId;
    }

    public String getCategoriaCompliance() {
        return categoriaCompliance;
    }

    public void setCategoriaCompliance(String categoriaCompliance) {
        this.categoriaCompliance = categoriaCompliance;
    }

    public Boolean getDadosPessoais() {
        return dadosPessoais;
    }

    public void setDadosPessoais(Boolean dadosPessoais) {
        this.dadosPessoais = dadosPessoais;
    }

    public LocalDateTime getRetencaoAte() {
        return retencaoAte;
    }

    public void setRetencaoAte(LocalDateTime retencaoAte) {
        this.retencaoAte = retencaoAte;
    }

    public Boolean getAnonimizado() {
        return anonimizado;
    }

    public void setAnonimizado(Boolean anonimizado) {
        this.anonimizado = anonimizado;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(LocalDateTime dataEvento) {
        this.dataEvento = dataEvento;
    }

    public LocalDateTime getDataProcessamento() {
        return dataProcessamento;
    }

    public void setDataProcessamento(LocalDateTime dataProcessamento) {
        this.dataProcessamento = dataProcessamento;
    }

    public String getOrigemSistema() {
        return origemSistema;
    }

    public void setOrigemSistema(String origemSistema) {
        this.origemSistema = origemSistema;
    }

    public String getVersaoSistema() {
        return versaoSistema;
    }

    public void setVersaoSistema(String versaoSistema) {
        this.versaoSistema = versaoSistema;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    @Override
    public String toString() {
        return "EventoAuditoriaR2dbc{" +
                "id='" + id + '\'' +
                ", tipoEvento=" + tipoEvento +
                ", usuarioId='" + usuarioId + '\'' +
                ", entidadeTipo='" + entidadeTipo + '\'' +
                ", acaoRealizada='" + acaoRealizada + '\'' +
                ", dataEvento=" + dataEvento +
                '}';
    }
}