package pe.edu.upeu.sysvstarbucks.dto.comprobante;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record BoletaVenta(String codigo, LocalDateTime fechaEmision, String nombreCliente,
                          String tipoDocumentoCliente, String numeroDocumentoCliente, String direccionCliente,
                          List<ItemBoleta> items, BigDecimal opGravadas, BigDecimal igv, BigDecimal total) {
    public static final String RUC_EMISOR = "20123456789";
    public static final String DIRECCION_EMISOR = "Av. Los Cafetos 456, Lima, Perú";
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.forLanguageTag("es-PE"));

    public BoletaVenta {
        codigo = codigo == null || codigo.isBlank() ? "B-0000" : codigo;
        fechaEmision = fechaEmision == null ? LocalDateTime.now() : fechaEmision;
        nombreCliente = nombreCliente == null || nombreCliente.isBlank() ? "Cliente ocasional" : nombreCliente;
        tipoDocumentoCliente = tipoDocumentoCliente == null || tipoDocumentoCliente.isBlank() ? "—" : tipoDocumentoCliente;
        numeroDocumentoCliente = numeroDocumentoCliente == null || numeroDocumentoCliente.isBlank() ? "—" : numeroDocumentoCliente;
        direccionCliente = direccionCliente == null || direccionCliente.isBlank() ? "—" : direccionCliente;
        items = items == null ? List.of() : List.copyOf(items);
        opGravadas = normalizar(opGravadas);
        igv = normalizar(igv);
        total = normalizar(total);
    }

    public String fechaEmisionTexto() { return fechaEmision.format(FORMATO_FECHA); }
    public String documentoClienteTexto() {
        return "—".equals(tipoDocumentoCliente) || "—".equals(numeroDocumentoCliente)
                ? "—" : tipoDocumentoCliente + " " + numeroDocumentoCliente;
    }
    public static String dinero(BigDecimal valor) { return normalizar(valor).toPlainString(); }
    private static BigDecimal normalizar(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
