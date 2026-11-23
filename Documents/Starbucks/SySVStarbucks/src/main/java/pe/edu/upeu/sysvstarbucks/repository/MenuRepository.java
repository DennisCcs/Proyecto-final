package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.model.Menu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuRepository {
    public List<Menu> findAll() {
        String sql = "SELECT id_menu, nombre FROM menu ORDER BY nombre";
        List<Menu> menus = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Menu menu = new Menu();
                menu.setIdMenu(result.getLong("id_menu"));
                menu.setNombre(result.getString("nombre"));
                menus.add(menu);
            }
            return menus;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los menús.", e);
        }
    }
}