package pe.edu.upeu.sysvstarbucks.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pe.edu.upeu.sysvstarbucks.repository.VentaRepository;

import java.math.BigDecimal;
import java.util.List;

/** Caché observable de ventas; la fuente persistente es H2. */
public final class VentaSessionStore {
    private static final ObservableList<VentaRegistrada> VENTAS = FXCollections.observableArrayList();

    private VentaSessionStore() { }

    public static ObservableList<VentaRegistrada> obtenerTodas() { return VENTAS; }

    public static void recargar() { VENTAS.setAll(new VentaRepository().findAll()); }

    public static void agregar(VentaRegistrada venta) { VENTAS.add(venta); }

    public record VentaRegistrada(String codigo, String cliente, String tipoDocumento, String documento,
                                  String vendedor, int productos, List<String> detalle,
                                  BigDecimal neto, BigDecimal igv, BigDecimal total) {
        public VentaRegistrada { detalle = List.copyOf(detalle); }

        public String documentoVisible() {
            return tipoDocumento.equals("—") ? "—" : tipoDocumento + " " + documento;
        }
    }

    public record DetalleVentaGuardado(String producto, String detalle, int cantidad,
                                       BigDecimal precioUnitario, BigDecimal subtotal) { }
}
