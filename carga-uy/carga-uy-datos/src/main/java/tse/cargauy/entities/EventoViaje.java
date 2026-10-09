package tse.cargauy.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_evento_viaje_id", columnNames = {"idEvento"}))
public class EventoViaje {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false) private String idEvento;      // UUID generado en el celular
    @Column(nullable = false) private Long idViaje;         // Long y no @ManyToOne: el viaje puede haber sido borrado y no queremos que se borre el evento
    @Column(nullable = false) private Long idChofer;        // id del usuario que lo mando
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoEventoViaje tipo;
    @Column(nullable = false) private Instant timestampGeneracion;
    @Column(nullable = false) private Instant timestampRecepcion;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ResultadoEvento resultado;
    private String detalle;

    public EventoViaje() {
    }

    public EventoViaje(String idEvento, Long idViaje, Long idChofer, TipoEventoViaje tipo, Instant timestampGeneracion, Instant timestampRecepcion, ResultadoEvento resultado, String detalle) {
        this.idEvento = idEvento;
        this.idViaje = idViaje;
        this.idChofer = idChofer;
        this.tipo = tipo;
        this.timestampGeneracion = timestampGeneracion;
        this.timestampRecepcion = timestampRecepcion;
        this.resultado = resultado;
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(String idEvento) {
        this.idEvento = idEvento;
    }

    public Long getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(Long idViaje) {
        this.idViaje = idViaje;
    }

    public Long getIdChofer() {
        return idChofer;
    }

    public void setIdChofer(Long idChofer) {
        this.idChofer = idChofer;
    }

    public TipoEventoViaje getTipo() {
        return tipo;
    }

    public void setTipo(TipoEventoViaje tipo) {
        this.tipo = tipo;
    }

    public Instant getTimestampGeneracion() {
        return timestampGeneracion;
    }

    public void setTimestampGeneracion(Instant timestampGeneracion) {
        this.timestampGeneracion = timestampGeneracion;
    }

    public Instant getTimestampRecepcion() {
        return timestampRecepcion;
    }

    public void setTimestampRecepcion(Instant timestampRecepcion) {
        this.timestampRecepcion = timestampRecepcion;
    }

    public ResultadoEvento getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoEvento resultado) {
        this.resultado = resultado;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
