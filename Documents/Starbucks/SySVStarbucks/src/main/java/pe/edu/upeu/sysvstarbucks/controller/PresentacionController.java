package pe.edu.upeu.sysvstarbucks.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import pe.edu.upeu.sysvstarbucks.model.Presentacion;
import pe.edu.upeu.sysvstarbucks.model.Producto;
import pe.edu.upeu.sysvstarbucks.model.UnidadMedida;
import pe.edu.upeu.sysvstarbucks.repository.PresentacionRepository;
import pe.edu.upeu.sysvstarbucks.repository.ProductoRepository;

import java.util.List;

public class PresentacionController {
    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private TextField txtNombre;
    @FXML private TextField txtCantidadMedida;
    @FXML private TextField txtIncremento;
    @FXML private Label lblMenu;
    @FXML private Label lblPrecioBase;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;
    @FXML private Button btnEliminar;
    @FXML private TableView<Presentacion> tablaPresentaciones;
    @FXML private TableColumn<Presentacion, String> colNombre;
    @FXML private TableColumn<Presentacion, String> colCantidad;
    @FXML private TableColumn<Presentacion, String> colUnidad;
    @FXML private TableColumn<Presentacion, String> colIncremento;
    @FXML private TableColumn<Presentacion, String> colPrecioFinal;

    private final ProductoRepository productoRepository = new ProductoRepository();
    private final PresentacionRepository presentacionRepository = new PresentacionRepository();
    private final ObservableList<Presentacion> opciones = FXCollections.observableArrayList();
    private Long idEnEdicion;

    @FXML
    private void initialize() {
        configurarTabla();
        cmbUnidad.setItems(FXCollections.observableArrayList("ml", "g", "unidad"));
        cmbProducto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Producto producto) {
                return producto == null ? "" : producto.getNombre();
            }
            @Override public Producto fromString(String texto) { return null; }
        });
        cmbProducto.valueProperty().addListener((obs, anterior, producto) -> seleccionarProducto(producto));
        tablaPresentaciones.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionada) -> {
            if (seleccionada != null) cargarEnFormulario(seleccionada);
        });

        bloquearFormulario(true);
        try {
            cmbProducto.setItems(FXCollections.observableArrayList(productoRepository.findAll()));
            if (!cmbProducto.getItems().isEmpty()) cmbProducto.getSelectionModel().selectFirst();
            else mostrarMensaje("Registra primero un producto en el catálogo.", false);
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(cell -> texto(cell.getValue().getNombreOpcion()));
        colCantidad.setCellValueFactory(cell -> texto(formatearCantidad(cell.getValue().getCantidadMedida())));
        colUnidad.setCellValueFactory(cell -> texto(cell.getValue().getUnidadMedida() == null
                ? "" : cell.getValue().getUnidadMedida().getNombreMedida()));
        colIncremento.setCellValueFactory(cell -> texto(dinero(cell.getValue().getIncrementoPrecio())));
        colPrecioFinal.setCellValueFactory(cell -> texto(dinero(
                cell.getValue().getProducto().getPrecioBase() + cell.getValue().getIncrementoPrecio())));
        tablaPresentaciones.setItems(opciones);
    }

    private void seleccionarProducto(Producto producto) {
        limpiarFormulario();
        opciones.clear();
        if (producto == null) {
            lblMenu.setText("—");
            lblPrecioBase.setText("S/ 0.00");
            bloquearFormulario(true);
            return;
        }

        lblMenu.setText(producto.getMenu() == null ? "—" : producto.getMenu().getNombre());
        lblPrecioBase.setText("S/ " + dinero(producto.getPrecioBase()));
        boolean esBebida = esBebida(producto);
        bloquearFormulario(!esBebida);

        try {
            List<Presentacion> guardadas = presentacionRepository.findByProducto(producto);
            opciones.setAll(guardadas);
            mostrarMensaje(esBebida
                    ? "Agrega aquí personalizaciones opcionales de esta bebida. Las tallas se mantienen fijas en ventas."
                    : "Las personalizaciones administrables se aplican solo a bebidas.", false);
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    private boolean esBebida(Producto producto) {
        return producto.getMenu() != null && producto.getMenu().getNombre() != null
                && producto.getMenu().getNombre().toLowerCase(java.util.Locale.ROOT).contains("bebida");
    }

    private void bloquearFormulario(boolean bloquear) {
        txtNombre.setDisable(bloquear);
        txtCantidadMedida.setDisable(bloquear);
        cmbUnidad.setDisable(bloquear);
        txtIncremento.setDisable(bloquear);
        btnGuardar.setDisable(bloquear);
        btnEliminar.setDisable(bloquear);
    }

    @FXML
    private void guardarPresentacion() {
        Producto producto = cmbProducto.getValue();
        if (producto == null || !esBebida(producto)) return;

        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String textoCantidad = txtCantidadMedida.getText() == null ? "" : txtCantidadMedida.getText().trim();
        String unidad = cmbUnidad.getValue();
        if (nombre.isEmpty() || unidad == null) {
            mostrarMensaje("Escribe el nombre de la personalización y selecciona la unidad.", true);
            return;
        }

        double cantidad;
        double adicional;
        try {
            cantidad = Double.parseDouble(textoCantidad.replace(',', '.'));
            adicional = Double.parseDouble(txtIncremento.getText().trim().replace(',', '.'));
            if (!Double.isFinite(cantidad) || cantidad <= 0 || !Double.isFinite(adicional) || adicional < 0) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException e) {
            mostrarMensaje("La medida debe ser mayor que cero y el precio adicional no puede ser negativo.", true);
            return;
        }

        boolean nombreDuplicado = opciones.stream().anyMatch(opcion ->
                opcion.getNombreOpcion().equalsIgnoreCase(nombre)
                        && !opcion.getIdPresentacion().equals(idEnEdicion));
        if (nombreDuplicado) {
            mostrarMensaje("Ese nombre de personalización ya existe para este producto.", true);
            return;
        }

        Presentacion opcion = idEnEdicion == null ? new Presentacion()
                : tablaPresentaciones.getSelectionModel().getSelectedItem();
        if (opcion == null) return;
        opcion.setProducto(producto);
        opcion.setNombreOpcion(nombre);
        opcion.setCantidadMedida(cantidad);
        opcion.setIncrementoPrecio(adicional);
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setNombreMedida(unidad);
        opcion.setUnidadMedida(unidadMedida);

        try {
            if (idEnEdicion == null) {
                presentacionRepository.save(opcion);
                mostrarMensaje("Personalización agregada.", false);
            } else {
                presentacionRepository.update(opcion);
                mostrarMensaje("Personalización actualizada.", false);
            }
            recargarOpciones(producto);
            limpiarFormulario();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    private void eliminarPresentacion() {
        Presentacion seleccionada = tablaPresentaciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Selecciona una personalización para quitarla.", true);
            return;
        }
        try {
            presentacionRepository.deleteById(seleccionada.getIdPresentacion());
            recargarOpciones(cmbProducto.getValue());
            limpiarFormulario();
            mostrarMensaje("Personalización eliminada.", false);
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    private void recargarOpciones(Producto producto) {
        opciones.setAll(presentacionRepository.findByProducto(producto));
    }

    @FXML
    private void limpiarFormulario() {
        idEnEdicion = null;
        txtNombre.clear();
        txtCantidadMedida.clear();
        txtIncremento.clear();
        cmbUnidad.getSelectionModel().clearSelection();
        if (tablaPresentaciones != null) tablaPresentaciones.getSelectionModel().clearSelection();
        if (btnGuardar != null) btnGuardar.setText("Agregar personalización");
    }

    private void cargarEnFormulario(Presentacion presentacion) {
        idEnEdicion = presentacion.getIdPresentacion();
        txtNombre.setText(presentacion.getNombreOpcion());
        txtCantidadMedida.setText(formatearCantidad(presentacion.getCantidadMedida()));
        cmbUnidad.setValue(presentacion.getUnidadMedida() == null
                ? null : presentacion.getUnidadMedida().getNombreMedida());
        txtIncremento.setText(dinero(presentacion.getIncrementoPrecio()));
        btnGuardar.setText("Guardar cambios");
        mostrarMensaje("Editando " + presentacion.getNombreOpcion() + ".", false);
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje == null ? "Ocurrió un error." : mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b3261e;" : "-fx-text-fill: #26734d;");
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }

    private static String formatearCantidad(Double cantidad) {
        if (cantidad == null) return "";
        return cantidad == Math.rint(cantidad) ? String.valueOf(cantidad.intValue()) : String.valueOf(cantidad);
    }

    private static String dinero(Double cantidad) {
        return String.format(java.util.Locale.US, "%.2f", cantidad == null ? 0.0 : cantidad);
    }
}
