package pe.edu.upeu.sysvstarbucks.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Bounds;
import javafx.stage.Popup;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import pe.edu.upeu.sysvstarbucks.model.Insumo;
import pe.edu.upeu.sysvstarbucks.model.Proveedor;
import pe.edu.upeu.sysvstarbucks.model.UnidadMedida;
import pe.edu.upeu.sysvstarbucks.repository.InsumoRepository;
import pe.edu.upeu.sysvstarbucks.service.InsumosSessionStore;
import pe.edu.upeu.sysvstarbucks.service.ProveedorSessionStore;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class InsumoController {
    @FXML private TextField txtNombre;
    private final Popup popupSugerencias = new Popup();
    private final javafx.scene.control.ListView<String> sugerenciasNombre = new javafx.scene.control.ListView<>();
    @FXML private TextField txtStockMinimo;
    @FXML private TextField txtNombrePaquete;
    @FXML private TextField txtContenidoPaquete;
    @FXML private TextField txtPrecioPaquete;
    @FXML private TextField txtBuscar;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private ComboBox<Proveedor> cmbProveedor;
    @FXML private Button btnGuardar;
    @FXML private Label lblMensaje;
    @FXML private Label lblConteo;
    @FXML private TableView<Insumo> tablaInsumos;
    @FXML private TableColumn<Insumo, String> colId;
    @FXML private TableColumn<Insumo, String> colNombre;
    @FXML private TableColumn<Insumo, String> colUnidad;
    @FXML private TableColumn<Insumo, String> colStock;
    @FXML private TableColumn<Insumo, String> colMinimo;
    @FXML private TableColumn<Insumo, String> colEstado;
    @FXML private TableColumn<Insumo, String> colProveedor;
    @FXML private TableColumn<Insumo, String> colPaquete;
    @FXML private TableColumn<Insumo, String> colPrecioPaquete;

    private final FilteredList<Insumo> insumos = new FilteredList<>(InsumosSessionStore.obtenerTodos(), item -> true);
    private final InsumoRepository insumoRepository = new InsumoRepository();
    private Long idEnEdicion;

    @FXML
    private void initialize() {
        try {
            ProveedorSessionStore.recargar();
            InsumosSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
        cmbUnidad.setItems(FXCollections.observableArrayList("g", "ml", "unidad"));
        cmbProveedor.setItems(ProveedorSessionStore.obtenerTodos());
        cmbProveedor.setConverter(new StringConverter<>() {
            @Override public String toString(Proveedor proveedor) {
                return proveedor == null ? "" : proveedor.getNombres();
            }
            @Override public Proveedor fromString(String texto) { return null; }
        });
        configurarAutocompletadoNombre();
        configurarTabla();
        tablaInsumos.setItems(insumos);
        tablaInsumos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, insumo) -> {
            if (insumo != null) cargarEnFormulario(insumo);
        });
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> filtrar(texto));
        actualizarConteo();
    }

    private void configurarAutocompletadoNombre() {
        sugerenciasNombre.setStyle("-fx-background-color: white; -fx-border-color: #b7c4bd;");
        sugerenciasNombre.setOnMouseClicked(event -> completarNombreSugerido());
        sugerenciasNombre.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) completarNombreSugerido();
        });
        popupSugerencias.setAutoHide(true);
        popupSugerencias.setHideOnEscape(true);
        popupSugerencias.setAutoFix(true);
        popupSugerencias.getContent().add(sugerenciasNombre);
        txtNombre.textProperty().addListener((obs, anterior, texto) -> actualizarSugerenciasNombre(texto));
    }

    private void actualizarSugerenciasNombre(String texto) {
        String consulta = normalizarNombre(texto);
        if (!txtNombre.isFocused() || consulta.isEmpty() || txtNombre.getScene() == null) {
            popupSugerencias.hide();
            return;
        }

        var opciones = InsumosSessionStore.obtenerTodos().stream()
                .filter(insumo -> idEnEdicion == null || !Objects.equals(insumo.getIdInsumo(), idEnEdicion))
                .map(Insumo::getNombre)
                .filter(nombre -> nombre != null && normalizarNombre(nombre).contains(consulta))
                .distinct()
                .sorted(Comparator.comparing((String nombre) -> !normalizarNombre(nombre).startsWith(consulta))
                        .thenComparing(String.CASE_INSENSITIVE_ORDER))
                .limit(8)
                .toList();
        if (opciones.isEmpty()) {
            popupSugerencias.hide();
            return;
        }

        sugerenciasNombre.setItems(FXCollections.observableArrayList(opciones));
        sugerenciasNombre.setPrefWidth(Math.max(240, txtNombre.getWidth()));
        sugerenciasNombre.setPrefHeight(Math.min(200, Math.max(32, opciones.size() * 28)));
        Bounds limites = txtNombre.localToScreen(txtNombre.getBoundsInLocal());
        if (limites != null && !popupSugerencias.isShowing()) {
            popupSugerencias.show(txtNombre.getScene().getWindow(), limites.getMinX(), limites.getMaxY());
        }
    }

    private void completarNombreSugerido() {
        String nombre = sugerenciasNombre.getSelectionModel().getSelectedItem();
        if (nombre == null) return;
        txtNombre.setText(nombre);
        txtNombre.positionCaret(nombre.length());
        popupSugerencias.hide();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(cell -> texto(String.valueOf(cell.getValue().getIdInsumo())));
        colNombre.setCellValueFactory(cell -> texto(cell.getValue().getNombre()));
        colUnidad.setCellValueFactory(cell -> texto(unidad(cell.getValue())));
        colStock.setCellValueFactory(cell -> texto(cantidad(cell.getValue().getStockActual())));
        colMinimo.setCellValueFactory(cell -> texto(cantidad(cell.getValue().getStockMinimo())));
        colEstado.setCellValueFactory(cell -> texto(estaBajoMinimo(cell.getValue()) ? "Reponer" : "Disponible"));
        colProveedor.setCellValueFactory(cell -> texto(nombreProveedor(cell.getValue())));
        colPaquete.setCellValueFactory(cell -> texto(cell.getValue().getNombrePaquete()));
        colPrecioPaquete.setCellValueFactory(cell -> texto(dinero(cell.getValue().getPrecioPaquete())));
    }

    @FXML
    private void guardarInsumo() {
        String nombre = limpio(txtNombre.getText());
        String descripcion = limpio(txtDescripcion.getText());
        String unidad = cmbUnidad.getValue();
        Proveedor proveedor = cmbProveedor.getValue();
        String nombrePaquete = limpio(txtNombrePaquete.getText());
        if (nombre.isEmpty() || unidad == null || proveedor == null || nombrePaquete.isEmpty()) {
            mostrarMensaje("Completa nombre, unidad, proveedor y nombre del paquete.", true);
            return;
        }

        double stockMinimo;
        double contenidoPaquete;
        double precioPaquete;
        try {
            stockMinimo = numero(txtStockMinimo.getText());
            contenidoPaquete = numero(txtContenidoPaquete.getText());
            precioPaquete = numero(txtPrecioPaquete.getText());
            if (!Double.isFinite(stockMinimo) || !Double.isFinite(contenidoPaquete)
                    || !Double.isFinite(precioPaquete) || stockMinimo < 0
                    || contenidoPaquete <= 0 || precioPaquete <= 0) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException e) {
            mostrarMensaje("Verifica el stock mínimo, el contenido y el precio del paquete.", true);
            return;
        }

        String claveNombre = normalizarNombre(nombre);
        boolean nombreRepetido = InsumosSessionStore.obtenerTodos().stream()
                .anyMatch(insumo -> normalizarNombre(insumo.getNombre()).equals(claveNombre)
                        && (idEnEdicion == null || !Objects.equals(insumo.getIdInsumo(), idEnEdicion)));
        if (nombreRepetido) {
            mostrarMensaje("Ya existe un insumo con ese nombre. Selecciónalo en la tabla para editarlo.", true);
            return;
        }

        boolean esNuevo = idEnEdicion == null;
        Insumo insumo;
        if (esNuevo) {
            insumo = new Insumo();
            insumo.setStockActual(0.0);
        } else {
            insumo = InsumosSessionStore.buscarPorId(idEnEdicion);
            if (insumo == null) {
                mostrarMensaje("No se encontró el insumo que se quiere actualizar.", true);
                return;
            }
        }
        UnidadMedida medida = insumo.getUnidadMedida();
        if (medida == null) medida = new UnidadMedida();
        medida.setNombreMedida(unidad);
        insumo.setUnidadMedida(medida);
        insumo.setNombre(nombre);
        insumo.setDescripcion(descripcion.isEmpty() ? null : descripcion);
        insumo.setStockMinimo(stockMinimo);
        insumo.setProveedor(proveedor);
        insumo.setNombrePaquete(nombrePaquete);
        insumo.setContenidoPorPaquete(contenidoPaquete);
        insumo.setPrecioPaquete(precioPaquete);
        try {
            if (esNuevo) insumoRepository.save(insumo);
            else insumoRepository.update(insumo);
            InsumosSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
            return;
        }

        tablaInsumos.refresh();
        limpiarFormulario();
        actualizarConteo();
        mostrarMensaje("Insumo guardado. Ya aparecerá bajo su proveedor en Nueva compra.", false);
    }

    @FXML
    private void eliminarInsumo() {
        Insumo seleccionado = tablaInsumos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un insumo de la tabla para eliminarlo.", true);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar insumo");
        confirmacion.setHeaderText("¿Eliminar " + seleccionado.getNombre() + "?");
        confirmacion.setContentText("Se quitará del catálogo y de los paquetes disponibles en Nueva compra.");
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                insumoRepository.deleteById(seleccionado.getIdInsumo());
                InsumosSessionStore.recargar();
            } catch (RuntimeException e) {
                mostrarMensaje(e.getMessage(), true);
                return;
            }
            limpiarFormulario();
            actualizarConteo();
            mostrarMensaje("Insumo eliminado.", false);
        }
    }

    @FXML
    private void limpiarFormulario() {
        popupSugerencias.hide();
        idEnEdicion = null;
        txtNombre.clear();
        txtStockMinimo.clear();
        txtNombrePaquete.clear();
        txtContenidoPaquete.clear();
        txtPrecioPaquete.clear();
        txtDescripcion.clear();
        cmbUnidad.getSelectionModel().clearSelection();
        cmbProveedor.getSelectionModel().clearSelection();
        tablaInsumos.getSelectionModel().clearSelection();
        btnGuardar.setText("Agregar insumo");
        lblMensaje.setText("");
    }

    private void cargarEnFormulario(Insumo insumo) {
        popupSugerencias.hide();
        idEnEdicion = insumo.getIdInsumo();
        txtNombre.setText(insumo.getNombre());
        txtStockMinimo.setText(cantidad(insumo.getStockMinimo()));
        txtNombrePaquete.setText(insumo.getNombrePaquete());
        txtContenidoPaquete.setText(cantidad(insumo.getContenidoPorPaquete()));
        txtPrecioPaquete.setText(dinero(insumo.getPrecioPaquete()));
        txtDescripcion.setText(insumo.getDescripcion());
        cmbUnidad.setValue(unidad(insumo));
        cmbProveedor.setValue(insumo.getProveedor());
        btnGuardar.setText("Guardar cambios");
        mostrarMensaje("Editando insumo. Stock actual: " + cantidad(insumo.getStockActual()) + " " + unidad(insumo), false);
    }

    private void filtrar(String consulta) {
        String filtro = consulta == null ? "" : consulta.trim().toLowerCase();
        insumos.setPredicate(insumo -> filtro.isEmpty()
                || contiene(insumo.getNombre(), filtro)
                || contiene(unidad(insumo), filtro)
                || contiene(insumo.getDescripcion(), filtro)
                || contiene(nombreProveedor(insumo), filtro)
                || contiene(insumo.getNombrePaquete(), filtro));
        actualizarConteo();
    }

    private void actualizarConteo() {
        lblConteo.setText(insumos.size() + " insumo(s)");
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b3261e;" : "-fx-text-fill: #26734d;");
    }

    private static boolean estaBajoMinimo(Insumo insumo) {
        return insumo.getStockActual() <= insumo.getStockMinimo();
    }

    private static String unidad(Insumo insumo) {
        return insumo.getUnidadMedida() == null ? "" : insumo.getUnidadMedida().getNombreMedida();
    }

    private static String nombreProveedor(Insumo insumo) {
        return insumo.getProveedor() == null ? "" : insumo.getProveedor().getNombres();
    }

    private static String limpio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String normalizarNombre(String valor) {
        if (valor == null) return "";
        String espacios = valor.trim().replaceAll("\\s+", " ");
        return Normalizer.normalize(espacios, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private static boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase().contains(filtro);
    }

    private static double numero(String valor) {
        return Double.parseDouble(limpio(valor).replace(',', '.'));
    }

    private static String cantidad(Double valor) {
        if (valor == null) return "0";
        return valor == Math.rint(valor) ? String.valueOf(valor.longValue())
                : String.format(java.util.Locale.US, "%.2f", valor);
    }

    private static String dinero(Double valor) {
        return String.format(java.util.Locale.US, "%.2f", valor == null ? 0.0 : valor);
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }
}

