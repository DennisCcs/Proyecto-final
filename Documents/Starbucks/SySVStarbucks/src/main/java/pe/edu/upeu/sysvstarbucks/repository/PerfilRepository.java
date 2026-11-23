package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.model.Perfil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PerfilRepository {
    public List<Perfil> findAll() {
        List<Perfil> perfiles = new ArrayList<>();
        String sql = "SELECT id_perfil, nombre, codigo FROM perfil ORDER BY id_perfil";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Perfil perfil = new Perfil();
                perfil.setIdPerfil(result.getLong("id_perfil"));
                perfil.setNombre(result.getString("nombre"));
                perfil.setCodigo(result.getString("codigo"));
                perfiles.add(perfil);
            }
            return perfiles;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los perfiles desde H2.", e);
        }
    }
}
