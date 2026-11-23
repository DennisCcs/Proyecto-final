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
import pe.edu.upeu.sysvstarbucks.service.VentaSessionStore;
import pe.edu.upeu.sysvstarbucks.service.VentaSessionStore.VentaRegistrada;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class HistorialVentasController {
    @FXML private TextField txtBuscar;
    @FXML private TableView<VentaRegistrada> tablaVentas;
    @FXML private TableColumn<VentaRegistrada, String> colBoleta;
    @FXML private TableColumn<VentaRegistrada, String> colCliente;
    @FXML private TableColumn<VentaRegistrada, String> colDocumento;
    @FXML private TableColumn<VentaRegistrada, String> colVendedor;
    @FXML private TableColumn<VentaRegistrada, String> colProductos;
    @FXML private TableColumn<VentaRegistrada, String> colNeto;
    @FXML private TableColumn<VentaRegistrada, String> colIgv;
    @FXML private TableColumn<VentaRegistrada, String> colTotal;
    @FXML private Button btnVerDetalle;
    @FXML private Label lblEstado;
    @FXML private Label lblCantidad;
    @FXML private Label lblTotalVendido;

    private final FilteredList<VentaRegistrada> ventas = new FilteredList<>(
            VentaSessionStore.obtenerTodas(), venta -> true);

    @FXML
    private void initialize() {
        VentaSessionStore.recargar();
        colBoleta.setCellValueFactory(cell -> texto(cell.getValue().codigo()));
        colCliente.setCellValueFactory(cell -> texto(cell.getValue().cliente()));
        colDocumento.setCellValueFactory(cell -> texto(cell.getValue().documentoVisible()));
        colVendedor.setCellValueFactory(cell -> texto(cell.getValue().vendedor()));
        colProductos.setCellValueFactory(cell -> texto(String.valueOf(cell.getValue().productos())));
        colNeto.setCellValueFactory(cell -> texto("S/ " + dinero(cell.getValue().neto())));
        colIgv.setCellValueFactory(cell -> texto("S/ " + dinero(cell.getValue().igv())));
        colTotal.setCellValueFactory(cell -> texto("S/ " + dinero(cell.getValue().total())));
        tablaVentas.setItems(ventas);
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> filtrar(texto));
        tablaVentas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionada) ->
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
        VentaRegistrada venta = tablaVentas.getSelectionModel().getSelectedItem();
        if (venta == null) return;
        Alert detalle = new Alert(Alert.AlertType.INFORMATION);
        detalle.setTitle("Boleta " + venta.codigo());
        detalle.setHeaderText("Detalle de la venta");
        detalle.setContentText("Cliente: " + venta.cliente()
                + "\nDocumento: " + venta.documentoVisible()
                + "\nAtendió: " + venta.vendedor()
                + "\n\n" + String.join("\n", venta.detalle())
                + "\n\nNeto: S/ " + dinero(venta.neto())
                + "\nIGV: S/ " + dinero(venta.igv())
                + "\nTotal: S/ " + dinero(venta.total()));
        detalle.showAndWait();
    }

    private void filtrar(String consulta) {
        String filtro = consulta == null ? "" : consulta.trim().toLowerCase(Locale.ROOT);
        ventas.setPredicate(venta -> filtro.isEmpty()
                || venta.codigo().toLowerCase(Locale.ROOT).contains(filtro)
                || venta.cliente().toLowerCase(Locale.ROOT).contains(filtro)
                || venta.documentoVisible().toLowerCase(Locale.ROOT).contains(filtro)
                || venta.vendedor().toLowerCase(Locale.ROOT).contains(filtro));
        actualizarResumen();
        lblEstado.setText(ventas.isEmpty() && !filtro.isEmpty() ? "No se encontraron ventas." : "");
    }

    private void actualizarResumen() {
        lblCantidad.setText(String.valueOf(ventas.size()));
        BigDecimal total = ventas.stream().map(VentaRegistrada::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalVendido.setText("S/ " + dinero(total));
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }

    private static String dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
