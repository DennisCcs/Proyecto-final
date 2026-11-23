package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.enums.TipoDocumento;
import pe.edu.upeu.sysvstarbucks.model.Insumo;
import pe.edu.upeu.sysvstarbucks.model.Proveedor;
import pe.edu.upeu.sysvstarbucks.model.UnidadMedida;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InsumoRepository {
    private static final String SELECT = """
        SELECT i.id_insumo, i.nombre, i.descripcion, i.stock_actual, i.stock_minimo,
               i.nombre_paquete, i.contenido_por_paquete, i.precio_paquete,
               u.id_unidad, u.nombre_medida,
               p.id_proveedor, p.numero_documento, p.tipo_documento, p.nombres,
               p.celular, p.correo, p.direccion
        FROM insumo i
        JOIN unidad_medida u ON u.id_unidad = i.id_unidad
        JOIN proveedor p ON p.id_proveedor = i.id_proveedor
        """;

    public List<Insumo> findAll() {
        List<Insumo> insumos = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT + " ORDER BY i.nombre");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) insumos.add(map(result));
            return insumos;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los insumos desde H2.", e);
        }
    }

    public Insumo save(Insumo insumo) {
        String sql = """
            INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad,
                                id_proveedor, nombre_paquete, contenido_por_paquete, precio_paquete)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParametros(connection, statement, insumo);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) insumo.setIdInsumo(keys.getLong(1));
            }
            return insumo;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el insumo en H2.", e);
        }
    }

    public Insumo update(Insumo insumo) {
        String sql = """
            UPDATE insumo
            SET nombre = ?, descripcion = ?, stock_actual = ?, stock_minimo = ?, id_unidad = ?,
                id_proveedor = ?, nombre_paquete = ?, contenido_por_paquete = ?, precio_paquete = ?
            WHERE id_insumo = ?
            """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParametros(connection, statement, insumo);
            statement.setLong(10, insumo.getIdInsumo());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el insumo que se quiere actualizar.");
            }
            return insumo;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el insumo en H2.", e);
        }
    }

    public void updateStock(Long idInsumo, double stockActual) {
        String sql = "UPDATE insumo SET stock_actual = ? WHERE id_insumo = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, BigDecimal.valueOf(stockActual));
            statement.setLong(2, idInsumo);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el insumo para actualizar su stock.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el stock del insumo en H2.", e);
        }
    }

    public void deleteById(Long id) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM insumo WHERE id_insumo = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el insumo que se quiere eliminar.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar el insumo. Puede estar relacionado con una compra.", e);
        }
    }

    private void setParametros(Connection connection, PreparedStatement statement, Insumo insumo) throws SQLException {
        statement.setString(1, insumo.getNombre());
        statement.setString(2, insumo.getDescripcion());
        statement.setBigDecimal(3, BigDecimal.valueOf(insumo.getStockActual()));
        statement.setBigDecimal(4, BigDecimal.valueOf(insumo.getStockMinimo()));
        statement.setLong(5, buscarUnidad(connection, insumo.getUnidadMedida().getNombreMedida()));
        statement.setLong(6, insumo.getProveedor().getIdProveedor());
        statement.setString(7, insumo.getNombrePaquete());
        statement.setBigDecimal(8, BigDecimal.valueOf(insumo.getContenidoPorPaquete()));
        statement.setBigDecimal(9, BigDecimal.valueOf(insumo.getPrecioPaquete()));
    }

    private long buscarUnidad(Connection connection, String nombreUnidad) throws SQLException {
        String sql = "SELECT id_unidad FROM unidad_medida WHERE LOWER(nombre_medida) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nombreUnidad);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("La unidad de medida no existe en H2.");
                return result.getLong("id_unidad");
            }
        }
    }

    private Insumo map(ResultSet result) throws SQLException {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setIdUnidad(result.getLong("id_unidad"));
        unidad.setNombreMedida(result.getString("nombre_medida"));

        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(result.getLong("id_proveedor"));
        proveedor.setNumeroDocumento(result.getString("numero_documento"));
        String tipo = result.getString("tipo_documento");
        proveedor.setTipoDocumento(tipo == null ? null : TipoDocumento.valueOf(tipo));
        proveedor.setNombres(result.getString("nombres"));
        proveedor.setCelular(result.getString("celular"));
        proveedor.setCorreo(result.getString("correo"));
        proveedor.setDireccion(result.getString("direccion"));

        Insumo insumo = new Insumo();
        insumo.setIdInsumo(result.getLong("id_insumo"));
        insumo.setNombre(result.getString("nombre"));
        insumo.setDescripcion(result.getString("descripcion"));
        insumo.setStockActual(result.getBigDecimal("stock_actual").doubleValue());
        insumo.setStockMinimo(result.getBigDecimal("stock_minimo").doubleValue());
        insumo.setUnidadMedida(unidad);
        insumo.setProveedor(proveedor);
        insumo.setNombrePaquete(result.getString("nombre_paquete"));
        insumo.setContenidoPorPaquete(result.getBigDecimal("contenido_por_paquete").doubleValue());
        insumo.setPrecioPaquete(result.getBigDecimal("precio_paquete").doubleValue());
        return insumo;
    }
}
