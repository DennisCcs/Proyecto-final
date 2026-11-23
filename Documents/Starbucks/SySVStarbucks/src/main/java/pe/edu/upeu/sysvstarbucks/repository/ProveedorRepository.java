package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.enums.TipoDocumento;
import pe.edu.upeu.sysvstarbucks.model.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProveedorRepository {
    private static final String SELECT = """
        SELECT id_proveedor, numero_documento, tipo_documento, nombres, celular, correo, direccion
        FROM proveedor
        ORDER BY nombres
        """;

    public List<Proveedor> findAll() {
        List<Proveedor> proveedores = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) proveedores.add(map(result));
            return proveedores;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los proveedores desde H2.", e);
        }
    }

    public Proveedor save(Proveedor proveedor) {
        String sql = "INSERT INTO proveedor (numero_documento, tipo_documento, nombres, celular, correo, direccion) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParametros(statement, proveedor);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) proveedor.setIdProveedor(keys.getLong(1));
            }
            return proveedor;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el proveedor en H2.", e);
        }
    }

    public Proveedor update(Proveedor proveedor) {
        String sql = "UPDATE proveedor SET numero_documento = ?, tipo_documento = ?, nombres = ?, celular = ?, correo = ?, direccion = ? WHERE id_proveedor = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParametros(statement, proveedor);
            statement.setLong(7, proveedor.getIdProveedor());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el proveedor que se quiere actualizar.");
            }
            return proveedor;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el proveedor en H2.", e);
        }
    }

    public void deleteById(Long id) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM proveedor WHERE id_proveedor = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el proveedor que se quiere eliminar.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar el proveedor. Revisa si tiene insumos o compras asociados.", e);
        }
    }

    private void setParametros(PreparedStatement statement, Proveedor proveedor) throws SQLException {
        statement.setString(1, proveedor.getNumeroDocumento());
        statement.setString(2, proveedor.getTipoDocumento() == null ? null : proveedor.getTipoDocumento().name());
        statement.setString(3, proveedor.getNombres());
        statement.setString(4, proveedor.getCelular());
        statement.setString(5, proveedor.getCorreo());
        statement.setString(6, proveedor.getDireccion());
    }

    private Proveedor map(ResultSet result) throws SQLException {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(result.getLong("id_proveedor"));
        proveedor.setNumeroDocumento(result.getString("numero_documento"));
        String tipo = result.getString("tipo_documento");
        proveedor.setTipoDocumento(tipo == null ? null : TipoDocumento.valueOf(tipo));
        proveedor.setNombres(result.getString("nombres"));
        proveedor.setCelular(result.getString("celular"));
        proveedor.setCorreo(result.getString("correo"));
        proveedor.setDireccion(result.getString("direccion"));
        return proveedor;
    }
}
