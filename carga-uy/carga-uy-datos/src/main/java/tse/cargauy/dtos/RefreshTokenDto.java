package tse.cargauy.dtos;

import java.io.Serializable;
import java.time.Instant;

import tse.cargauy.entities.EstadoRefreshToken;

public class RefreshTokenDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String hash;
    private Long idUsuario;
    private String dispositivo;
    private EstadoRefreshToken estado;
    private Instant emitido;
    private Instant ultimoUso;
    private Instant expira;

    public RefreshTokenDto() {
    }

    public RefreshTokenDto(String hash, Long idUsuario, String dispositivo, EstadoRefreshToken estado, Instant emitido,
            Instant ultimoUso, Instant expira) {
        this.hash = hash;
        this.idUsuario = idUsuario;
        this.dispositivo = dispositivo;
        this.estado = estado;
        this.emitido = emitido;
        this.ultimoUso = ultimoUso;
        this.expira = expira;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    public EstadoRefreshToken getEstado() {
        return estado;
    }

    public void setEstado(EstadoRefreshToken estado) {
        this.estado = estado;
    }

    public Instant getEmitido() {
        return emitido;
    }

    public void setEmitido(Instant emitido) {
        this.emitido = emitido;
    }

    public Instant getUltimoUso() {
        return ultimoUso;
    }

    public void setUltimoUso(Instant ultimoUso) {
        this.ultimoUso = ultimoUso;
    }

    public Instant getExpira() {
        return expira;
    }

    public void setExpira(Instant expira) {
        this.expira = expira;
    }
}
