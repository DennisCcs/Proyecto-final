package pe.edu.upeu.sysvstarbucks.dto.comprobante;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ItemBoleta(String producto, String detalle, int cantidad,
                         BigDecimal precioUnitario, BigDecimal subtotal) {
    public ItemBoleta {
        producto = producto == null || producto.isBlank() ? "Producto" : producto.trim();
        detalle = detalle == null ? "" : detalle.trim();
        precioUnitario = normalizar(precioUnitario);
        subtotal = normalizar(subtotal);
    }
    public String descripcion() {
        return detalle.isBlank() ? producto : producto + " - " + detalle;
    }
    private static BigDecimal normalizar(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
