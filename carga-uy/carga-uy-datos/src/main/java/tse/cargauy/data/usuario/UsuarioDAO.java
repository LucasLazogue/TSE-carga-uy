package tse.cargauy.data.usuario;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Chofer;
import tse.cargauy.entities.Ciudadano;
import tse.cargauy.entities.Funcionario;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.Usuario;

@Stateless
public class UsuarioDAO implements UsuarioDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public UsuarioDto getCiudadanoByCedula(String cedula) {
        return getByCedula(Ciudadano.class, cedula);
    }

    @Override
    public UsuarioDto getFuncionarioByCedula(String cedula) {
        return getByCedula(Funcionario.class, cedula);
    }

    @Override
    public UsuarioDto addCiudadano(String cedula, String correo) {
        Ciudadano ciudadano = new Ciudadano(cedula, correo);
        ciudadano.getRoles().add(getRol(Rol.CIUDADANO));
        entityManager.persist(ciudadano);
        return Serializers.toDto(ciudadano);
    }

    @Override
    public void addFuncionario(String cedula) {
        Funcionario funcionario = new Funcionario(cedula);
        funcionario.getRoles().add(getRol(Rol.FUNCIONARIO));
        entityManager.persist(funcionario);
    }

    @Override
    public void addChofer(String cedula, String correo) {
        Chofer chofer = new Chofer(cedula, correo);
        chofer.getRoles().add(getRol(Rol.CIUDADANO));
        chofer.getRoles().add(getRol(Rol.CHOFER));
        entityManager.persist(chofer);
    }

    @Override
    public List<String> getRoles() {
        return entityManager.createQuery("SELECT r.nombre FROM Rol r ORDER BY r.nombre", String.class).getResultList();
    }

    @Override
    public void addRol(String nombre) {
        entityManager.persist(new Rol(nombre));
    }

    private Rol getRol(String nombre) {
        return entityManager.createQuery("SELECT r FROM Rol r WHERE r.nombre = :nombre", Rol.class)
                .setParameter("nombre", nombre)
                .getSingleResult();
    }

    private <T extends Usuario> UsuarioDto getByCedula(Class<T> tipo, String cedula) {
        List<T> usuarios = entityManager.createQuery(
                        "SELECT u FROM " + tipo.getSimpleName() + " u WHERE u.cedula = :cedula", tipo)
                .setParameter("cedula", cedula)
                .getResultList();
        if (usuarios.isEmpty()) {
            return null;
        }
        return Serializers.toDto(usuarios.get(0));
    }
}
