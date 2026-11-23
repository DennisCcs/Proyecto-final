package pe.edu.upeu.sysvstarbucks.components.comprob;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pe.edu.upeu.sysvstarbucks.dto.comprobante.BoletaVenta;
import pe.edu.upeu.sysvstarbucks.dto.comprobante.ItemBoleta;

public final class BoletaView {
    private static final String PRIMARIO = "#2c3e50";
    private static final String ACENTO = "#c0392b";
    private static final String CLARO = "#f4f6f7";
    private static final String BORDE = "#dfe4e6";
    private final BoletaVenta boleta;

    public BoletaView(BoletaVenta boleta) { this.boleta = boleta; }

    public VBox construir() {
        VBox pagina = new VBox(18);
        pagina.setPadding(new Insets(28));
        pagina.setMinWidth(720); pagina.setPrefWidth(720); pagina.setMaxWidth(720);
        pagina.setStyle("-fx-background-color: white;");
        pagina.getChildren().addAll(cabecera(), datosCliente(), detalle(), totales(), pie());
        return pagina;
    }

    private HBox cabecera() {
        VBox emisor = new VBox(4,
                etiqueta("STARBUCKS", 22, true, PRIMARIO),
                etiqueta("RUC: " + BoletaVenta.RUC_EMISOR, 12, true, PRIMARIO),
                etiqueta(BoletaVenta.DIRECCION_EMISOR, 11, false, PRIMARIO));
        HBox.setHgrow(emisor, Priority.ALWAYS);
        VBox documento = new VBox(6,
                etiqueta("BOLETA DE VENTA", 16, true, ACENTO),
                etiqueta("R.U.C. " + BoletaVenta.RUC_EMISOR, 13, true, PRIMARIO),
                etiqueta("Nro. " + boleta.codigo(), 13, true, ACENTO),
                etiqueta("Fecha: " + boleta.fechaEmisionTexto() + " | Moneda: PEN", 11, false, PRIMARIO));
        documento.setAlignment(Pos.CENTER);
        documento.setPadding(new Insets(12));
        documento.setStyle("-fx-border-color: " + ACENTO + "; -fx-border-width: 2;");
        HBox fila = new HBox(20, emisor, documento);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private VBox datosCliente() {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(7); grid.setPadding(new Insets(10));
        grid.setStyle("-fx-border-color: " + BORDE + ";");
        grid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints());
        grid.addRow(0, etiqueta(boleta.tipoDocumentoCliente(), 11, true, PRIMARIO),
                etiqueta(boleta.numeroDocumentoCliente(), 11, false, PRIMARIO));
        grid.addRow(1, etiqueta("Cliente", 11, true, PRIMARIO),
                etiqueta(boleta.nombreCliente(), 11, false, PRIMARIO));
        grid.addRow(2, etiqueta("Dirección", 11, true, PRIMARIO),
                etiqueta(boleta.direccionCliente(), 11, false, PRIMARIO));
        return new VBox(6, etiqueta("DATOS DEL CLIENTE", 12, true, PRIMARIO), grid);
    }

    private VBox detalle() {
        GridPane encabezado = new GridPane();
        encabezado.setPadding(new Insets(8, 9, 8, 9));
        encabezado.setStyle("-fx-background-color: " + PRIMARIO + ";");
        definirColumnas(encabezado);
        encabezado.addRow(0,
                celda("CANT.", true, Pos.CENTER),
                celda("DESCRIPCIÓN", true, Pos.CENTER_LEFT),
                celda("P. UNIT.", true, Pos.CENTER_RIGHT),
                celda("SUBTOTAL", true, Pos.CENTER_RIGHT));

        VBox filas = new VBox();
        filas.setStyle("-fx-border-color: " + BORDE + ";");
        for (int i = 0; i < boleta.items().size(); i++) {
            ItemBoleta item = boleta.items().get(i);
            GridPane fila = new GridPane();
            fila.setPadding(new Insets(8, 9, 8, 9));
            definirColumnas(fila);
            fila.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : CLARO) + ";");
            fila.addRow(0,
                    celda(String.valueOf(item.cantidad()), false, Pos.CENTER),
                    celda(item.descripcion(), false, Pos.CENTER_LEFT),
                    celda("S/ " + BoletaVenta.dinero(item.precioUnitario()), false, Pos.CENTER_RIGHT),
                    celda("S/ " + BoletaVenta.dinero(item.subtotal()), false, Pos.CENTER_RIGHT));
            filas.getChildren().add(fila);
        }
        return new VBox(6, etiqueta("DETALLE", 12, true, PRIMARIO), encabezado, filas);
    }

    private void definirColumnas(GridPane grid) {
        grid.getColumnConstraints().addAll(
                new ColumnConstraints(48), new ColumnConstraints(340),
                new ColumnConstraints(105), new ColumnConstraints(110));
    }

    private HBox totales() {
        VBox izquierdo = new VBox(5,
                etiqueta("SON:", 12, true, PRIMARIO),
                etiqueta("TOTAL: S/ " + BoletaVenta.dinero(boleta.total()), 12, true, PRIMARIO),
                etiqueta("Gracias por tu compra", 12, false, PRIMARIO));
        HBox.setHgrow(izquierdo, Priority.ALWAYS);
        VBox montos = new VBox(4,
                filaMonto("Op. Gravadas", boleta.opGravadas(), false),
                filaMonto("IGV (18%)", boleta.igv(), false),
                filaMonto("TOTAL", boleta.total(), true));
        montos.setPrefWidth(290);
        return new HBox(22, izquierdo, montos);
    }

    private HBox filaMonto(String concepto, java.math.BigDecimal monto, boolean total) {
        String colorTexto = total ? "white" : PRIMARIO;
        HBox fila = new HBox(12);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(6, 9, 6, 9));
        fila.setStyle("-fx-background-color: " + (total ? ACENTO : CLARO) + ";");
        Label nombre = etiqueta(concepto, total ? 12 : 11, true, colorTexto);
        Label valor = etiqueta("S/ " + BoletaVenta.dinero(monto), total ? 12 : 11, total, colorTexto);
        valor.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(valor, Priority.ALWAYS);
        fila.getChildren().addAll(nombre, valor);
        return fila;
    }

    private VBox pie() {
        Label nota = etiqueta("Representación impresa de la BOLETA DE VENTA.",
                10, false, PRIMARIO);
        nota.setAlignment(Pos.CENTER);
        nota.setMaxWidth(Double.MAX_VALUE);
        VBox pie = new VBox(nota);
        pie.setPadding(new Insets(12, 0, 0, 0));
        pie.setStyle("-fx-border-color: " + BORDE + " transparent transparent transparent;");
        return pie;
    }

    private Label celda(String texto, boolean encabezado, Pos alineacion) {
        Label label = etiqueta(texto, encabezado ? 10 : 11, encabezado, encabezado ? "white" : PRIMARIO);
        label.setAlignment(alineacion); label.setMaxWidth(Double.MAX_VALUE); label.setWrapText(true);
        return label;
    }

    private Label etiqueta(String texto, double size, boolean bold, String color) {
        Label label = new Label(texto == null ? "" : texto);
        label.setStyle("-fx-font-family: 'Arial'; -fx-font-size: " + size + "px; -fx-text-fill: " + color
                + (bold ? "; -fx-font-weight: bold;" : ";"));
        label.setWrapText(true);
        return label;
    }
}
