package pe.edu.upeu.sysvstarbucks.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import pe.edu.upeu.sysvstarbucks.service.CompraSessionStore;
import pe.edu.upeu.sysvstarbucks.service.CompraSessionStore.CompraGuardada;
import pe.edu.upeu.sysvstarbucks.service.CompraSessionStore.DetalleCompraGuardado;

import java.math.BigDecimal;

public class HistorialComprasController {
    @FXML private TextField txtBuscar;
    @FXML private TableView<CompraGuardada> tablaCompras;
    @FXML private TableColumn<CompraGuardada, String> colCodigo;
    @FXML private TableColumn<CompraGuardada, String> colProveedor;
    @FXML private TableColumn<CompraGuardada, String> colPaquetes;
    @FXML private TableColumn<CompraGuardada, String> colLineas;
    @FXML private TableColumn<CompraGuardada, String> colNeto;
    @FXML private TableColumn<CompraGuardada, String> colIgv;
    @FXML private TableColumn<CompraGuardada, String> colTotal;
    @FXML private Button btnVerDetalle;
    @FXML private Label lblEstado;
    @FXML private Label lblCantidad;
    @FXML private Label lblTotalComprado;

    private final FilteredList<CompraGuardada> compras = new FilteredList<>(
            CompraSessionStore.obtenerCompras(), compra -> true);

    @FXML
    private void initialize() {
        CompraSessionStore.recargar();
        colCodigo.setCellValueFactory(cell -> texto(cell.getValue().codigo()));
        colProveedor.setCellValueFactory(cell -> texto(cell.getValue().proveedor()));
        colPaquetes.setCellValueFactory(cell -> texto(String.valueOf(cell.getValue().cantidadPaquetes())));
        colLineas.setCellValueFactory(cell -> texto(String.valueOf(cell.getValue().detalles().size())));
        colNeto.setCellValueFactory(cell -> texto(dinero(cell.getValue().neto())));
        colIgv.setCellValueFactory(cell -> texto(dinero(cell.getValue().igv())));
        colTotal.setCellValueFactory(cell -> texto(dinero(cell.getValue().total())));
        tablaCompras.setItems(compras);

        txtBuscar.textProperty().addListener((obs, anterior, valor) -> filtrar(valor));
        tablaCompras.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionada) ->
                btnVerDetalle.setDisable(seleccionada == null));
        actualizarResumen();
    }

    @FXML
    private void limpiarBusqueda() {
        txtBuscar.clear();
        lblEstado.setText("");
    }

    @FXML
    private void verDetalle() {
        CompraGuardada compra = tablaCompras.getSelectionModel().getSelectedItem();
        if (compra == null) return;

        StringBuilder contenido = new StringBuilder();
        for (DetalleCompraGuardado detalle : compra.detalles()) {
            contenido.append(detalle.cantidadPaquetes()).append(" × ")
                    .append(detalle.paquete()).append(" — ").append(detalle.insumo())
                    .append(" (contenido: ").append(detalle.contenido()).append(")")
                    .append("\nPrecio por paquete: S/ ").append(dinero(detalle.precioPaquete()))
                    .append(" | Subtotal: S/ ").append(dinero(detalle.subtotal())).append("\n\n");
        }
        contenido.append("Neto: S/ ").append(dinero(compra.neto()))
                .append("\nIGV: S/ ").append(dinero(compra.igv()))
                .append("\nTotal: S/ ").append(dinero(compra.total()));

        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle de compra " + compra.codigo());
        alerta.setHeaderText(compra.codigo() + " · " + compra.proveedor());
        alerta.setContentText(contenido.toString());
        alerta.showAndWait();
    }

    private void filtrar(String consulta) {
        String filtro = consulta == null ? "" : consulta.trim().toLowerCase();
        compras.setPredicate(compra -> filtro.isEmpty()
                || compra.codigo().toLowerCase().contains(filtro)
                || compra.proveedor().toLowerCase().contains(filtro));
        actualizarResumen();
        lblEstado.setText(compras.isEmpty() && !filtro.isEmpty() ? "No se encontraron compras." : "");
    }

    private void actualizarResumen() {
        lblCantidad.setText(String.valueOf(compras.size()));
        BigDecimal total = compras.stream().map(CompraGuardada::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalComprado.setText("S/ " + dinero(total));
    }

    private static String dinero(BigDecimal valor) {
        return String.format(java.util.Locale.US, "%.2f", valor.doubleValue());
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }
}
