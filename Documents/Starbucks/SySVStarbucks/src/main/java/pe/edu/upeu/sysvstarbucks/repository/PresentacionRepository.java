package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.model.Presentacion;
import pe.edu.upeu.sysvstarbucks.model.Producto;
import pe.edu.upeu.sysvstarbucks.model.UnidadMedida;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistencia de las personalizaciones opcionales asociadas a cada bebida. */
public class PresentacionRepository {
    private static final String SELECT_POR_PRODUCTO = """
        SELECT p.id_presentacion, p.id_producto, p.nombre_opcion, p.incremento_precio,
               p.stock, p.cantidad_medida, u.id_unidad, u.nombre_medida
        FROM presentacion p
        LEFT JOIN unidad_medida u ON u.id_unidad = p.id_unidad
        WHERE p.id_producto = ?
        ORDER BY p.nombre_opcion
        """;

    public List<Presentacion> findByProducto(Producto producto) {
        List<Presentacion> opciones = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_POR_PRODUCTO)) {
            statement.setLong(1, producto.getIdProducto());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) opciones.add(mapPresentacion(result, producto));
            }
            return opciones;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar las personalizaciones del producto.", e);
        }
    }

    public Presentacion save(Presentacion presentacion) {
        String sql = """
            INSERT INTO presentacion
                (id_producto, nombre_opcion, incremento_precio, stock, cantidad_medida, id_unidad)
            VALUES (?, ?, ?, 0, ?, ?)
            """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            completarParametros(connection, statement, presentacion);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) presentacion.setIdPresentacion(keys.getLong(1));
            }
            return presentacion;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la personalización. Comprueba que no esté repetida.", e);
        }
    }

    public Presentacion update(Presentacion presentacion) {
        String sql = """
            UPDATE presentacion
            SET nombre_opcion = ?, incremento_precio = ?, cantidad_medida = ?, id_unidad = ?
            WHERE id_presentacion = ? AND id_producto = ?
            """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, presentacion.getNombreOpcion());
            statement.setDouble(2, presentacion.getIncrementoPrecio());
            statement.setDouble(3, presentacion.getCantidadMedida());
            statement.setLong(4, buscarUnidad(connection, presentacion.getUnidadMedida().getNombreMedida()));
            statement.setLong(5, presentacion.getIdPresentacion());
            statement.setLong(6, presentacion.getProducto().getIdProducto());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró la personalización que se quiere actualizar.");
            }
            return presentacion;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar la personalización.", e);
        }
    }

    public void deleteById(Long idPresentacion) {
        String sql = "DELETE FROM presentacion WHERE id_presentacion = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, idPresentacion);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró la personalización que se quiere eliminar.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar la personalización.", e);
        }
    }

    private void completarParametros(Connection connection, PreparedStatement statement,
                                     Presentacion presentacion) throws SQLException {
        statement.setLong(1, presentacion.getProducto().getIdProducto());
        statement.setString(2, presentacion.getNombreOpcion());
        statement.setDouble(3, presentacion.getIncrementoPrecio());
        statement.setDouble(4, presentacion.getCantidadMedida());
        statement.setLong(5, buscarUnidad(connection, presentacion.getUnidadMedida().getNombreMedida()));
    }

    private long buscarUnidad(Connection connection, String nombreUnidad) throws SQLException {
        String sql = "SELECT id_unidad FROM unidad_medida WHERE LOWER(nombre_medida) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nombreUnidad);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("La unidad de medida no existe en el catálogo.");
                }
                return result.getLong("id_unidad");
            }
        }
    }

    private Presentacion mapPresentacion(ResultSet result, Producto producto) throws SQLException {
        Presentacion presentacion = new Presentacion();
        presentacion.setIdPresentacion(result.getLong("id_presentacion"));
        presentacion.setProducto(producto);
        presentacion.setNombreOpcion(result.getString("nombre_opcion"));
        presentacion.setIncrementoPrecio(result.getBigDecimal("incremento_precio").doubleValue());
        presentacion.setStock(result.getBigDecimal("stock").doubleValue());
        if (result.getBigDecimal("cantidad_medida") != null) {
            presentacion.setCantidadMedida(result.getBigDecimal("cantidad_medida").doubleValue());
        }
        if (result.getObject("id_unidad") != null) {
            UnidadMedida unidad = new UnidadMedida();
            unidad.setIdUnidad(result.getLong("id_unidad"));
            unidad.setNombreMedida(result.getString("nombre_medida"));
            presentacion.setUnidadMedida(unidad);
        }
        return presentacion;
    }
}
