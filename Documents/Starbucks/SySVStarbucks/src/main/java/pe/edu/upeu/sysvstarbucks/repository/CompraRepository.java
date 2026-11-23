package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.service.CompraSessionStore.CompraGuardada;
import pe.edu.upeu.sysvstarbucks.service.CompraSessionStore.DetalleCompraGuardado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CompraRepository {
    public CompraGuardada registrarCompra(Long idUsuario, Long idProveedor, String proveedor,
                                           List<DetalleCompraGuardado> detalles,
                                           BigDecimal neto, BigDecimal igv, BigDecimal total) {
        String sqlCompra = "INSERT INTO compra (neto, igv, total, id_usuario, id_proveedor) VALUES (?, ?, ?, ?, ?)";
        String sqlDetalle = """
            INSERT INTO compra_detalle
                (id_compra, id_insumo, cantidad, precio_unitario, subtotal,
                 nombre_paquete, nombre_insumo, contenido_paquete, unidad_medida)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        String sqlStock = "UPDATE insumo SET stock_actual = stock_actual + ? WHERE id_insumo = ?";

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long idCompra;
                try (PreparedStatement statement = connection.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setBigDecimal(1, neto);
                    statement.setBigDecimal(2, igv);
                    statement.setBigDecimal(3, total);
                    statement.setLong(4, idUsuario);
                    statement.setLong(5, idProveedor);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("H2 no devolvió el ID de la compra.");
                        idCompra = keys.getLong(1);
                    }
                }

                try (PreparedStatement detalleStatement = connection.prepareStatement(sqlDetalle);
                     PreparedStatement stockStatement = connection.prepareStatement(sqlStock)) {
                    for (DetalleCompraGuardado detalle : detalles) {
                        detalleStatement.setLong(1, idCompra);
                        detalleStatement.setLong(2, detalle.idInsumo());
                        detalleStatement.setBigDecimal(3, BigDecimal.valueOf(detalle.cantidadPaquetes()));
                        detalleStatement.setBigDecimal(4, detalle.precioPaquete());
                        detalleStatement.setBigDecimal(5, detalle.subtotal());
                        detalleStatement.setString(6, detalle.paquete());
                        detalleStatement.setString(7, detalle.insumo());
                        detalleStatement.setBigDecimal(8, BigDecimal.valueOf(detalle.contenidoPorPaquete()));
                        detalleStatement.setString(9, detalle.unidadBase());
                        detalleStatement.addBatch();

                        BigDecimal aumentoStock = BigDecimal.valueOf(detalle.contenidoPorPaquete())
                                .multiply(BigDecimal.valueOf(detalle.cantidadPaquetes()));
                        stockStatement.setBigDecimal(1, aumentoStock);
                        stockStatement.setLong(2, detalle.idInsumo());
                        stockStatement.addBatch();
                    }
                    detalleStatement.executeBatch();
                    for (int actualizadas : stockStatement.executeBatch()) {
                        if (actualizadas == 0) throw new SQLException("No se encontró un insumo para actualizar el stock.");
                    }
                }

                connection.commit();
                return new CompraGuardada(codigo(idCompra), proveedor, detalles, neto, igv, total, false);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo registrar la compra y actualizar el stock en H2.", e);
        }
    }

    public List<CompraGuardada> findAll() {
        String sql = """
            SELECT c.id_compra, c.neto, c.igv, c.total, p.nombres AS proveedor
            FROM compra c
            JOIN proveedor p ON p.id_proveedor = c.id_proveedor
            ORDER BY c.id_compra DESC
            """;
        try (Connection connection = DatabaseConfig.getConnection()) {
            List<CompraEncabezado> encabezados = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    encabezados.add(new CompraEncabezado(result.getLong("id_compra"),
                            result.getString("proveedor"), result.getBigDecimal("neto"),
                            result.getBigDecimal("igv"), result.getBigDecimal("total")));
                }
            }
            List<CompraGuardada> compras = new ArrayList<>();
            for (CompraEncabezado encabezado : encabezados) {
                compras.add(new CompraGuardada(codigo(encabezado.id()), encabezado.proveedor(),
                        buscarDetalles(connection, encabezado.id()), encabezado.neto(), encabezado.igv(),
                        encabezado.total(), false));
            }
            return compras;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cargar el historial de compras desde H2.", e);
        }
    }

    private List<DetalleCompraGuardado> buscarDetalles(Connection connection, long idCompra) throws SQLException {
        String sql = """
            SELECT cd.id_insumo, COALESCE(cd.nombre_paquete, i.nombre_paquete) AS paquete,
                   COALESCE(cd.nombre_insumo, i.nombre) AS insumo,
                   COALESCE(cd.contenido_paquete, i.contenido_por_paquete) AS contenido,
                   COALESCE(cd.unidad_medida, u.nombre_medida) AS unidad,
                   cd.cantidad, cd.precio_unitario, cd.subtotal
            FROM compra_detalle cd
            JOIN insumo i ON i.id_insumo = cd.id_insumo
            JOIN unidad_medida u ON u.id_unidad = i.id_unidad
            WHERE cd.id_compra = ?
            ORDER BY cd.id_compra_detalle
            """;
        List<DetalleCompraGuardado> detalles = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, idCompra);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    double contenido = result.getBigDecimal("contenido").doubleValue();
                    String unidad = result.getString("unidad");
                    int cantidad = result.getBigDecimal("cantidad").intValue();
                    detalles.add(new DetalleCompraGuardado(result.getLong("id_insumo"),
                            result.getString("paquete"), result.getString("insumo"),
                            cantidad(contenido) + " " + unidad, contenido, unidad, cantidad,
                            result.getBigDecimal("precio_unitario"), result.getBigDecimal("subtotal")));
                }
            }
        }
        return detalles;
    }

    private String codigo(long id) { return String.format(Locale.ROOT, "C-%04d", id); }

    private String cantidad(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor)
                : BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private record CompraEncabezado(long id, String proveedor, BigDecimal neto, BigDecimal igv, BigDecimal total) { }
}
