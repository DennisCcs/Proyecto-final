package pe.edu.upeu.sysvstarbucks.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pe.edu.upeu.sysvstarbucks.model.Perfil;
import pe.edu.upeu.sysvstarbucks.model.Usuario;
import pe.edu.upeu.sysvstarbucks.repository.PerfilRepository;
import pe.edu.upeu.sysvstarbucks.repository.UsuarioRepository;

/** Caché observable de usuarios y perfiles persistidos en H2. */
public final class UsuarioSessionStore {
    private static final ObservableList<Usuario> USUARIOS = FXCollections.observableArrayList();
    private static final ObservableList<Perfil> PERFILES = FXCollections.observableArrayList();
    private static final UsuarioRepository USUARIO_REPOSITORY = new UsuarioRepository();
    private static final PerfilRepository PERFIL_REPOSITORY = new PerfilRepository();

    private UsuarioSessionStore() { }

    public static ObservableList<Usuario> obtenerTodos() { return USUARIOS; }

    public static ObservableList<Perfil> obtenerPerfiles() { return PERFILES; }

    public static void recargar() {
        PERFILES.setAll(PERFIL_REPOSITORY.findAll());
        USUARIOS.setAll(USUARIO_REPOSITORY.findAll());
    }

    public static Usuario buscarPorId(Long id) {
        if (id == null) return null;
        return USUARIOS.stream().filter(usuario -> id.equals(usuario.getIdUsuario())).findFirst().orElse(null);
    }
}
