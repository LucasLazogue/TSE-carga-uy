package tse.cargauy.data.usuario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Chofer;
import tse.cargauy.entities.Ciudadano;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.Funcionario;
import tse.cargauy.entities.Responsable;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.Usuario;
import tse.cargauy.entities.UsuarioBackoffice;
import tse.cargauy.entities.VinculoEmpresa;

@Stateless
public class UsuarioDAO implements UsuarioDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public UsuarioDto getById(Long id) {
        Usuario usuario = entityManager.find(Usuario.class, id);
        return usuario == null ? null : toDto(usuario);
    }

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
        return toDto(ciudadano);
    }

    @Override
    public void addFuncionario(String cedula) {
        Funcionario funcionario = new Funcionario(cedula);
        funcionario.getRoles().add(getRol(Rol.FUNCIONARIO));
        entityManager.persist(funcionario);
    }

    @Override
    public PaginaDto<UsuarioDto> getFuncionarios(Paginacion paginacion) {
        List<UsuarioDto> funcionarios = entityManager.createQuery(
                        "SELECT f FROM Funcionario f ORDER BY f.cedula", Funcionario.class)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultStream()
                .map(this::toDto)
                .toList();
        long total = entityManager.createQuery("SELECT COUNT(f) FROM Funcionario f", Long.class).getSingleResult();
        return new PaginaDto<>(funcionarios, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public void deleteFuncionario(String cedula) {
        entityManager.createQuery("SELECT f FROM Funcionario f WHERE f.cedula = :cedula", Funcionario.class)
                .setParameter("cedula", cedula)
                .getResultStream()
                .findFirst()
                .ifPresent(entityManager::remove);
    }

    @Override
    public void deleteUsuarioBackoffice(String usuario) {
        entityManager.createQuery("SELECT u FROM UsuarioBackoffice u WHERE u.usuario = :usuario", UsuarioBackoffice.class)
                .setParameter("usuario", usuario)
                .getResultStream()
                .findFirst()
                .ifPresent(entityManager::remove);
    }

    @Override
    public UsuarioDto saveUsuarioBackoffice(String usuario, List<String> roles) {
        UsuarioBackoffice backoffice = entityManager.createQuery(
                        "SELECT u FROM UsuarioBackoffice u WHERE u.usuario = :usuario", UsuarioBackoffice.class)
                .setParameter("usuario", usuario)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (backoffice == null) {
            backoffice = new UsuarioBackoffice(usuario);
            entityManager.persist(backoffice);
        }
        backoffice.getRoles().clear();
        for (String rol : roles) {
            backoffice.getRoles().add(getRol(rol));
        }
        return toDto(backoffice);
    }

    @Override
    public void addChofer(Long idCiudadano, Long idEmpresa, LocalDate fechaDesde) {
        entityManager.persist(new Chofer(entityManager.find(Ciudadano.class, idCiudadano),
                entityManager.find(Empresa.class, idEmpresa), fechaDesde));
    }

    @Override
    public void addResponsable(Long idCiudadano, Long idEmpresa, LocalDate fechaDesde) {
        entityManager.persist(new Responsable(entityManager.find(Ciudadano.class, idCiudadano),
                entityManager.find(Empresa.class, idEmpresa), fechaDesde));
    }

    @Override
    public Long getChoferVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha) {
        return getVigente(Chofer.class, idCiudadano, idEmpresa, fecha);
    }

    @Override
    public Long getResponsableVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha) {
        return getVigente(Responsable.class, idCiudadano, idEmpresa, fecha);
    }

    @Override
    public PaginaDto<UsuarioDto> getChoferes(Long idEmpresa, String cedula, LocalDate fecha, Paginacion paginacion) {
        String desde = " FROM Chofer c WHERE c.empresa.id = :idEmpresa AND c.ciudadano.cedula LIKE :cedula"
                + " AND c.fechaDesde <= :fecha AND (c.fechaHasta IS NULL OR c.fechaHasta >= :fecha)";
        String patron = (cedula == null ? "" : cedula.replaceAll("\\D", "")) + "%";

        List<UsuarioDto> choferes = entityManager.createQuery(
                        "SELECT c.ciudadano" + desde + " ORDER BY c.ciudadano.cedula", Ciudadano.class)
                .setParameter("idEmpresa", idEmpresa)
                .setParameter("cedula", patron)
                .setParameter("fecha", fecha)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultStream()
                .map(Serializers::toDto)
                .toList();
        long total = entityManager.createQuery("SELECT COUNT(c)" + desde, Long.class)
                .setParameter("idEmpresa", idEmpresa)
                .setParameter("cedula", patron)
                .setParameter("fecha", fecha)
                .getSingleResult();
        return new PaginaDto<>(choferes, total, paginacion.getPagina(), paginacion.getTamanio());
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
        return toDto(usuarios.get(0));
    }

    private UsuarioDto toDto(Usuario usuario) {
        UsuarioDto dto = Serializers.toDto(usuario);
        if (usuario instanceof Ciudadano) {
            LocalDate hoy = LocalDate.now();
            List<String> roles = new ArrayList<>(dto.getRoles());
            if (!getVigentes(Chofer.class, usuario.getId(), hoy).isEmpty()) {
                roles.add(Rol.CHOFER);
            }
            if (!getVigentes(Responsable.class, usuario.getId(), hoy).isEmpty()) {
                roles.add(Rol.RESPONSABLE);
            }
            dto.setRoles(roles.stream().sorted().toList());
        }
        return dto;
    }

    private <T extends VinculoEmpresa> Long getVigente(Class<T> tipo, Long idCiudadano, Long idEmpresa, LocalDate fecha) {
        return getVigentes(tipo, idCiudadano, fecha).stream()
                .filter(v -> v.getEmpresa().getId().equals(idEmpresa))
                .map(VinculoEmpresa::getId)
                .findFirst()
                .orElse(null);
    }

    private <T extends VinculoEmpresa> List<T> getVigentes(Class<T> tipo, Long idCiudadano, LocalDate fecha) {
        return entityManager.createQuery(
                        "SELECT v FROM " + tipo.getSimpleName() + " v WHERE v.ciudadano.id = :idCiudadano"
                                + " AND v.fechaDesde <= :fecha AND (v.fechaHasta IS NULL OR v.fechaHasta >= :fecha)", tipo)
                .setParameter("idCiudadano", idCiudadano)
                .setParameter("fecha", fecha)
                .getResultList();
    }
}
