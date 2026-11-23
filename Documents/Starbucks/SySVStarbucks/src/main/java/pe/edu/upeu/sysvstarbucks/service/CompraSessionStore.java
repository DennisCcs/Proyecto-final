package pe.edu.upeu.sysvstarbucks.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pe.edu.upeu.sysvstarbucks.repository.CompraRepository;

import java.math.BigDecimal;
import java.util.List;

/** Caché observable del historial de compras; la fuente persistente es H2. */
public final class CompraSessionStore {
    private static final ObservableList<CompraGuardada> COMPRAS = FXCollections.observableArrayList();

    private CompraSessionStore() { }

    public static ObservableList<CompraGuardada> obtenerCompras() { return COMPRAS; }

    public static void recargar() { COMPRAS.setAll(new CompraRepository().findAll()); }

    public static void agregar(CompraGuardada compra) { COMPRAS.add(compra); }

    public record DetalleCompraGuardado(Long idInsumo, String paquete, String insumo, String contenido,
                                        double contenidoPorPaquete, String unidadBase, int cantidadPaquetes,
                                        BigDecimal precioPaquete, BigDecimal subtotal) { }

    public record CompraGuardada(String codigo, String proveedor, List<DetalleCompraGuardado> detalles,
                                 BigDecimal neto, BigDecimal igv, BigDecimal total, boolean muestra) {
        public CompraGuardada { detalles = List.copyOf(detalles); }

        public int cantidadPaquetes() {
            return detalles.stream().mapToInt(DetalleCompraGuardado::cantidadPaquetes).sum();
        }
    }
}
