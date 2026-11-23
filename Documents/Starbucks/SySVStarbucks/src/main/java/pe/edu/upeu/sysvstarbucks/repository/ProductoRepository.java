package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.model.Menu;
import pe.edu.upeu.sysvstarbucks.model.Producto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoRepository implements ICrudGenericoRepository<Producto, Long> {
    private static final String SELECT_PRODUCTOS = """
        SELECT p.id_producto, p.nombre, p.precio_base, p.descripcion,
               m.id_menu, m.nombre AS nombre_menu
        FROM producto p
        JOIN menu m ON m.id_menu = p.id_menu
        """;

    @Override
    public Producto save(Producto producto) {
        String sql = "INSERT INTO producto (nombre, precio_base, descripcion, id_menu) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setProductoParameters(statement, producto);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    producto.setIdProducto(keys.getLong(1));
                }
            }
            return producto;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el producto.", e);
        }
    }

    @Override
    public Producto update(Producto producto) {
        String sql = "UPDATE producto SET nombre = ?, precio_base = ?, descripcion = ?, id_menu = ? WHERE id_producto = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setProductoParameters(statement, producto);
            statement.setLong(5, producto.getIdProducto());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el producto que se quiere actualizar.");
            }
            return producto;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el producto.", e);
        }
    }

    @Override
    public Optional<Producto> findById(Long id) {
        String sql = SELECT_PRODUCTOS + " WHERE p.id_producto = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapProducto(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo buscar el producto.", e);
        }
    }

    @Override
    public List<Producto> findAll() {
        String sql = SELECT_PRODUCTOS + " ORDER BY p.id_producto";
        List<Producto> productos = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                productos.add(mapProducto(result));
            }
            return productos;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los productos.", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM producto WHERE id_producto = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se encontró el producto que se quiere eliminar.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar el producto.", e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM producto WHERE id_producto = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo comprobar el producto.", e);
        }
    }

    private void setProductoParameters(PreparedStatement statement, Producto producto) throws SQLException {
        statement.setString(1, producto.getNombre());
        statement.setBigDecimal(2, BigDecimal.valueOf(producto.getPrecioBase()));
        statement.setString(3, producto.getDescripcion());
        statement.setLong(4, producto.getMenu().getIdMenu());
    }

    private Producto mapProducto(ResultSet result) throws SQLException {
        Menu menu = new Menu();
        menu.setIdMenu(result.getLong("id_menu"));
        menu.setNombre(result.getString("nombre_menu"));

        Producto producto = new Producto();
        producto.setIdProducto(result.getLong("id_producto"));
        producto.setNombre(result.getString("nombre"));
        producto.setPrecioBase(result.getBigDecimal("precio_base").doubleValue());
        producto.setDescripcion(result.getString("descripcion"));
        producto.setMenu(menu);
        return producto;
    }
}