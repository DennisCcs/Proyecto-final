package pe.edu.upeu.sysvstarbucks.repository;

import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.service.VentaSessionStore.DetalleVentaGuardado;
import pe.edu.upeu.sysvstarbucks.service.VentaSessionStore.VentaRegistrada;

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

public class VentaRepository {
    public VentaRegistrada registrarVenta(Long idUsuario, String nombreCliente, String tipoDocumento,
                                           String documento, String vendedor,
                                           List<DetalleVentaGuardado> detalles,
                                           BigDecimal neto, BigDecimal igv, BigDecimal total) {
        String sqlVenta = "INSERT INTO venta (neto, igv, total, id_usuario, numero_documento_cliente) VALUES (?, ?, ?, ?, ?)";
        String sqlDetalle = """
            INSERT INTO detalle_venta
                (id_venta, id_presentacion, cantidad, precio_aplicado, subtotal, producto_nombre, detalle)
            VALUES (?, NULL, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long idVenta;
                try (PreparedStatement statement = connection.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setBigDecimal(1, neto);
                    statement.setBigDecimal(2, igv);
                    statement.setBigDecimal(3, total);
                    statement.setLong(4, idUsuario);
                    statement.setString(5, documento);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("H2 no devolvió el ID de la venta.");
                        idVenta = keys.getLong(1);
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(sqlDetalle)) {
                    for (DetalleVentaGuardado detalle : detalles) {
                        statement.setLong(1, idVenta);
                        statement.setInt(2, detalle.cantidad());
                        statement.setBigDecimal(3, detalle.precioUnitario());
                        statement.setBigDecimal(4, detalle.subtotal());
                        statement.setString(5, detalle.producto());
                        statement.setString(6, detalle.detalle());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
                return construirRegistro(idVenta, nombreCliente, tipoDocumento, documento, vendedor,
                        detalles, neto, igv, total);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo registrar la venta en H2.", e);
        }
    }

    public List<VentaRegistrada> findAll() {
        String sql = """
            SELECT v.id_venta, v.neto, v.igv, v.total, v.numero_documento_cliente,
                   u.usuario AS vendedor, c.tipo_documento, c.nombres AS nombre_cliente
            FROM venta v
            JOIN usuario u ON u.id_usuario = v.id_usuario
            LEFT JOIN cliente c ON c.numero_documento = v.numero_documento_cliente
            ORDER BY v.id_venta DESC
            """;
        try (Connection connection = DatabaseConfig.getConnection()) {
            List<VentaEncabezado> encabezados = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    encabezados.add(new VentaEncabezado(result.getLong("id_venta"),
                            result.getString("nombre_cliente"), result.getString("tipo_documento"),
                            result.getString("numero_documento_cliente"), result.getString("vendedor"),
                            result.getBigDecimal("neto"), result.getBigDecimal("igv"), result.getBigDecimal("total")));
                }
            }
            List<VentaRegistrada> ventas = new ArrayList<>();
            for (VentaEncabezado encabezado : encabezados) {
                List<String> detalle = buscarDetalle(connection, encabezado.id());
                ventas.add(new VentaRegistrada(codigo(encabezado.id()),
                        encabezado.cliente() == null ? "Cliente ocasional" : encabezado.cliente(),
                        encabezado.tipoDocumento() == null ? "—" : encabezado.tipoDocumento(),
                        encabezado.documento() == null ? "—" : encabezado.documento(),
                        encabezado.vendedor(), detalle.size(), detalle,
                        encabezado.neto(), encabezado.igv(), encabezado.total()));
            }
            return ventas;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cargar el historial de ventas desde H2.", e);
        }
    }

    private List<String> buscarDetalle(Connection connection, long idVenta) throws SQLException {
        String sql = """
            SELECT producto_nombre, detalle, cantidad, precio_aplicado, subtotal
            FROM detalle_venta
            WHERE id_venta = ?
            ORDER BY id_detalle_venta
            """;
        List<String> lineas = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, idVenta);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String producto = result.getString("producto_nombre");
                    String detalle = result.getString("detalle");
                    BigDecimal precio = result.getBigDecimal("precio_aplicado");
                    BigDecimal subtotal = result.getBigDecimal("subtotal");
                    int cantidad = result.getInt("cantidad");
                    lineas.add(formatearLinea(cantidad,
                            producto == null ? "Producto" : producto,
                            detalle == null ? "" : detalle, precio, subtotal));
                }
            }
        }
        return lineas;
    }

    private VentaRegistrada construirRegistro(long idVenta, String nombreCliente, String tipoDocumento,
                                               String documento, String vendedor,
                                               List<DetalleVentaGuardado> detalles,
                                               BigDecimal neto, BigDecimal igv, BigDecimal total) {
        List<String> lineas = detalles.stream().map(detalle -> formatearLinea(detalle.cantidad(), detalle.producto(),
                detalle.detalle(), detalle.precioUnitario(), detalle.subtotal())).toList();
        String cliente = nombreCliente == null || nombreCliente.isBlank() ? "Cliente ocasional" : nombreCliente;
        String tipo = tipoDocumento == null || tipoDocumento.isBlank() ? "—" : tipoDocumento;
        String doc = documento == null || documento.isBlank() ? "—" : documento;
        String nombreVendedor = vendedor == null || vendedor.isBlank() ? "Vendedor" : vendedor;
        return new VentaRegistrada(codigo(idVenta), cliente, tipo, doc, nombreVendedor,
                detalles.size(), lineas, neto, igv, total);
    }

    private String formatearLinea(int cantidad, String producto, String detalle,
                                  BigDecimal precio, BigDecimal subtotal) {
        return String.format(Locale.US, "%d × %s — %s | Unitario: S/ %s | Subtotal: S/ %s",
                cantidad, producto, detalle, dinero(precio), dinero(subtotal));
    }

    private String codigo(long id) { return String.format(Locale.ROOT, "B-%04d", id); }

    private String dinero(BigDecimal monto) {
        return monto == null ? "0.00" : monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private record VentaEncabezado(long id, String cliente, String tipoDocumento, String documento,
                                   String vendedor, BigDecimal neto, BigDecimal igv, BigDecimal total) { }
}
