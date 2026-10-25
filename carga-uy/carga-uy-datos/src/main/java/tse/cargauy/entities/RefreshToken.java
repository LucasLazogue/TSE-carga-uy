package tse.cargauy.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// el refresh token de la app movil (RFC 6749 1.5). se guarda el hash y no el token: si la base se filtra,
// los tokens no sirven
@Entity
// el indice: cada login revoca los activos del mismo chofer y celular, y sin el recorreria toda la tabla
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = {"hash"}),
        indexes = @Index(name = "ix_refresh_token_usuario_dispositivo", columnList = "idUsuario, dispositivo"))
public class RefreshToken {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false) private String hash;          // SHA-256 del token, en base64url
    @Column(nullable = false) private Long idUsuario;
    @Column(nullable = false) private String dispositivo;   // el celular al que se le entrego
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoRefreshToken estado;
    @Column(nullable = false) private Instant emitido;
    @Column(nullable = false) private Instant ultimoUso;    // para vencerlo por inactividad (RFC 9700 4.14)
    @Column(nullable = false) private Instant expira;

    public RefreshToken() {
    }

    public RefreshToken(String hash, Long idUsuario, String dispositivo, EstadoRefreshToken estado, Instant emitido,
            Instant ultimoUso, Instant expira) {
        this.hash = hash;
        this.idUsuario = idUsuario;
        this.dispositivo = dispositivo;
        this.estado = estado;
        this.emitido = emitido;
        this.ultimoUso = ultimoUso;
        this.expira = expira;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
