package tse.cargauy.data.refreshtoken;

import java.time.Instant;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.RefreshTokenDto;
import tse.cargauy.entities.EstadoRefreshToken;
import tse.cargauy.entities.RefreshToken;

@Stateless
public class RefreshTokenDAO implements RefreshTokenDAOLocal {
    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public void crear(RefreshTokenDto refreshToken) {
        entityManager.persist(Serializers.toEntity(refreshToken));
    }

    // uno activo por chofer y celular: un login nuevo deja sin efecto al anterior
    @Override
    public void revocarActivos(Long idUsuario, String dispositivo) {
        entityManager.createQuery("UPDATE RefreshToken r SET r.estado = :revocado"
                        + " WHERE r.idUsuario = :idUsuario AND r.dispositivo = :dispositivo AND r.estado = :activo")
                .setParameter("revocado", EstadoRefreshToken.REVOCADO)
                .setParameter("activo", EstadoRefreshToken.ACTIVO)
                .setParameter("idUsuario", idUsuario)
                .setParameter("dispositivo", dispositivo)
                .executeUpdate();
    }

    @Override
    public RefreshTokenDto getByHash(String hash) {
        return entityManager.createQuery("SELECT r FROM RefreshToken r WHERE r.hash = :hash", RefreshToken.class)
                .setParameter("hash", hash)
                .getResultStream()
                .findFirst()
                .map(Serializers::toDto)
                .orElse(null);
    }

    @Override
    public void registrarUso(String hash, Instant ahora) {
        entityManager.createQuery("UPDATE RefreshToken r SET r.ultimoUso = :ahora WHERE r.hash = :hash")
                .setParameter("ahora", ahora)
                .setParameter("hash", hash)
                .executeUpdate();
    }

    // ultimoUso viejo alcanza para saber que ya no sirve (vence a los 7 dias sin uso), este revocado o no
    @Override
    public int borrarViejos(Instant limite) {
        return entityManager.createQuery("DELETE FROM RefreshToken r WHERE r.ultimoUso < :limite OR r.expira < :limite")
                .setParameter("limite", limite)
                .executeUpdate();
    }

    @Override
    public void revocar(String hash) {
        entityManager.createQuery("UPDATE RefreshToken r SET r.estado = :revocado WHERE r.hash = :hash")
                .setParameter("revocado", EstadoRefreshToken.REVOCADO)
                .setParameter("hash", hash)
                .executeUpdate();
    }
}
