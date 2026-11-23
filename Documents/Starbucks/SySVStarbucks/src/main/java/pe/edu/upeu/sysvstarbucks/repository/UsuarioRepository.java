package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.enums.EstadoUsuario;
import pe.edu.upeu.sysvstarbucks.model.Perfil;
import pe.edu.upeu.sysvstarbucks.model.Usuario;
import pe.edu.upeu.sysvstarbucks.utils.ClaveSegura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioRepository {
    private static final String SELECT = """
        SELECT u.id_usuario, u.usuario, u.clave_hash, u.estado,
               p.id_perfil, p.nombre AS nombre_perfil, p.codigo AS codigo_perfil
        FROM usuario u
        JOIN perfil p ON p.id_perfil = u.id_perfil
        """;

    public List<Usuario> findAll() {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT + " ORDER BY u.id_usuario");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) usuarios.add(map(result));
            return usuarios;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los usuarios desde H2.", e);
        }
    }

    public Usuario save(Usuario usuario) {
        String sql = "INSERT INTO usuario (usuario, clave_hash, estado, id_perfil) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParametros(statement, usuario);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) usuario.setIdUsuario(keys.getLong(1));
            }
            return usuario;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el usuario en H2.", e);
        }
    }

    public Usuario update(Usuario usuario) {
        String sql = "UPDATE usuario SET usuario = ?, clave_hash = ?, estado = ?, id_perfil = ? WHERE id_usuario = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParametros(statement, usuario);
            statement.setLong(5, usuario.getIdUsuario());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el usuario que se quiere actualizar.");
            }
            return usuario;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el usuario en H2.", e);
        }
    }

    public void deleteById(Long id) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM usuario WHERE id_usuario = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el usuario que se quiere eliminar.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar el usuario. Puede estar relacionado con ventas o compras.", e);
        }
    }

    /** Crea la cuenta inicial solo cuando aún no hay ningún administrador persistido. */
    public void asegurarAdministradorInicial() {
        String perfilSql = "SELECT id_perfil FROM perfil WHERE codigo = 'ADMINISTRADOR'";
        String existeAdminSql = """
            SELECT COUNT(*) FROM usuario u JOIN perfil p ON p.id_perfil = u.id_perfil
            WHERE p.codigo = 'ADMINISTRADOR'
            """;
        String existeUsuarioSql = "SELECT COUNT(*) FROM usuario WHERE LOWER(usuario) = LOWER(?)";
        String insertarSql = "INSERT INTO usuario (usuario, clave_hash, estado, id_perfil) VALUES (?, ?, 'ACTIVO', ?)";

        try (Connection connection = DatabaseConfig.getConnection()) {
            long idPerfil;
            try (PreparedStatement statement = connection.prepareStatement(perfilSql);
                 ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("No existe el perfil Administrador en H2.");
                idPerfil = result.getLong(1);
            }

            try (PreparedStatement statement = connection.prepareStatement(existeAdminSql);
                 ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getLong(1) > 0) return;
            }

            try (PreparedStatement statement = connection.prepareStatement(existeUsuarioSql)) {
                statement.setString(1, "dennis");
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && result.getLong(1) > 0) return;
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(insertarSql)) {
                statement.setString(1, "dennis");
                statement.setString(2, ClaveSegura.hash("dennis123"));
                statement.setLong(3, idPerfil);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo preparar el administrador inicial en H2.", e);
        }
    }

    private void setParametros(PreparedStatement statement, Usuario usuario) throws SQLException {
        statement.setString(1, usuario.getUsuario());
        statement.setString(2, usuario.getClaveHash());
        statement.setString(3, usuario.getEstado().name());
        statement.setLong(4, usuario.getPerfil().getIdPerfil());
    }

    private Usuario map(ResultSet result) throws SQLException {
        Perfil perfil = new Perfil();
        perfil.setIdPerfil(result.getLong("id_perfil"));
        perfil.setNombre(result.getString("nombre_perfil"));
        perfil.setCodigo(result.getString("codigo_perfil"));

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(result.getLong("id_usuario"));
        usuario.setUsuario(result.getString("usuario"));
        usuario.setClaveHash(result.getString("clave_hash"));
        usuario.setEstado(EstadoUsuario.valueOf(result.getString("estado")));
        usuario.setPerfil(perfil);
        return usuario;
    }
}
