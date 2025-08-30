package br.tec.facilitaservicos.auditoria.infraestrutura.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.auditoria.dominio.entidade.EventoAuditoriaR2dbc;

@Service
public class HashIntegridadeService {
    public String calcularHashEvento(EventoAuditoriaR2dbc e) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String base = String.join("|",
                String.valueOf(e.getId()),
                String.valueOf(e.getTipoEvento()),
                String.valueOf(e.getUsuarioId()),
                String.valueOf(e.getEntidadeTipo()),
                String.valueOf(e.getEntidadeId()),
                String.valueOf(e.getAcaoRealizada()),
                String.valueOf(e.getDadosAntes()),
                String.valueOf(e.getDadosDepois()),
                String.valueOf(e.getDataEvento())
            );
            byte[] digest = md.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            return Integer.toHexString(String.valueOf(e.getId()).hashCode());
        }
    }
}

