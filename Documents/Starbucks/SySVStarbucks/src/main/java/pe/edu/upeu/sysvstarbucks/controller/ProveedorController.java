package pe.edu.upeu.sysvstarbucks.controller;

import javafx.beans.property.SimpleStringProperty;
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
import pe.edu.upeu.sysvstarbucks.enums.TipoDocumento;
import pe.edu.upeu.sysvstarbucks.model.Proveedor;
import pe.edu.upeu.sysvstarbucks.repository.ProveedorRepository;
import pe.edu.upeu.sysvstarbucks.service.InsumosSessionStore;
import pe.edu.upeu.sysvstarbucks.service.ProveedorSessionStore;

import java.util.Optional;

public class ProveedorController {
    @FXML private ComboBox<TipoDocumento> cmbTipoDocumento;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtNombres;
    @FXML private TextField txtCelular;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtBuscar;
    @FXML private TextArea txtDireccion;
    @FXML private Button btnGuardar;
    @FXML private Label lblMensaje;
    @FXML private Label lblConteo;
    @FXML private TableView<Proveedor> tablaProveedores;
    @FXML private TableColumn<Proveedor, String> colTipo;
    @FXML private TableColumn<Proveedor, String> colDocumento;
    @FXML private TableColumn<Proveedor, String> colNombres;
    @FXML private TableColumn<Proveedor, String> colCelular;
    @FXML private TableColumn<Proveedor, String> colCorreo;
    @FXML private TableColumn<Proveedor, String> colDireccion;

    private final FilteredList<Proveedor> proveedores = new FilteredList<>(ProveedorSessionStore.obtenerTodos(), proveedor -> true);
    private final ProveedorRepository proveedorRepository = new ProveedorRepository();
    private Long idEnEdicion;

    @FXML
    private void initialize() {
        try {
            ProveedorSessionStore.recargar();
            InsumosSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
        cmbTipoDocumento.setItems(FXCollections.observableArrayList(TipoDocumento.values()));
        cmbTipoDocumento.setValue(TipoDocumento.RUC);
        cmbTipoDocumento.setConverter(new StringConverter<>() {
            @Override public String toString(TipoDocumento tipo) { return tipo == null ? "" : tipo.name(); }
            @Override public TipoDocumento fromString(String texto) {
                try { return TipoDocumento.valueOf(texto); }
                catch (IllegalArgumentException e) { return null; }
            }
        });

        colTipo.setCellValueFactory(cell -> texto(tipo(cell.getValue())));
        colDocumento.setCellValueFactory(cell -> texto(cell.getValue().getNumeroDocumento()));
        colNombres.setCellValueFactory(cell -> texto(cell.getValue().getNombres()));
        colCelular.setCellValueFactory(cell -> texto(cell.getValue().getCelular()));
        colCorreo.setCellValueFactory(cell -> texto(cell.getValue().getCorreo()));
        colDireccion.setCellValueFactory(cell -> texto(cell.getValue().getDireccion()));
        tablaProveedores.setItems(proveedores);
        tablaProveedores.getSelectionModel().selectedItemProperty().addListener((obs, anterior, proveedor) -> {
            if (proveedor != null) cargarEnFormulario(proveedor);
        });
        txtBuscar.textProperty().addListener((obs, anterior, valor) -> filtrar(valor));
        actualizarConteo();
    }

    @FXML
    private void guardarProveedor() {
        TipoDocumento tipo = cmbTipoDocumento.getValue();
        String documento = limpio(txtDocumento.getText());
        String nombres = limpio(txtNombres.getText());
        String celular = limpio(txtCelular.getText());
        String correo = limpio(txtCorreo.getText());
        String direccion = limpio(txtDireccion.getText());

        if (nombres.isEmpty()) {
            mostrarMensaje("El nombre o razón social es obligatorio.", true);
            return;
        }
        Proveedor existente = idEnEdicion == null ? null : ProveedorSessionStore.buscarPorId(idEnEdicion);
        boolean sinDocumentoGuardado = existente != null && existente.getNumeroDocumento() == null
                && tipo == null && documento.isEmpty();
        if (tipo == null || documento.isEmpty()) {
            if (!sinDocumentoGuardado) {
                mostrarMensaje("Selecciona el tipo e ingresa el documento del proveedor.", true);
                return;
            }
        } else if (!documentoValido(tipo, documento)) {
            mostrarMensaje(tipo == TipoDocumento.DNI ? "El DNI debe tener 8 dígitos." :
                    tipo == TipoDocumento.RUC ? "El RUC debe tener 11 dígitos." : "Revisa el número de documento.", true);
            return;
        }

        boolean documentoDuplicado = !documento.isEmpty() && ProveedorSessionStore.obtenerTodos().stream()
                .anyMatch(proveedor -> documento.equals(proveedor.getNumeroDocumento())
                        && !java.util.Objects.equals(proveedor.getIdProveedor(), idEnEdicion));
        if (documentoDuplicado) {
            mostrarMensaje("Ya existe un proveedor con ese documento.", true);
            return;
        }
        if (!correo.isEmpty() && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            mostrarMensaje("Escribe un correo válido o deja el campo vacío.", true);
            return;
        }

        Proveedor proveedor = idEnEdicion == null ? new Proveedor() : existente;
        if (proveedor == null) {
            mostrarMensaje("No se encontró el proveedor que se quiere actualizar.", true);
            return;
        }
        proveedor.setTipoDocumento(tipo);
        proveedor.setNumeroDocumento(documento.isEmpty() ? null : documento);
        proveedor.setNombres(nombres);
        proveedor.setCelular(celular.isEmpty() ? null : celular);
        proveedor.setCorreo(correo.isEmpty() ? null : correo);
        proveedor.setDireccion(direccion.isEmpty() ? null : direccion);
        try {
            if (idEnEdicion == null) proveedorRepository.save(proveedor);
            else proveedorRepository.update(proveedor);
            ProveedorSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
            return;
        }
        tablaProveedores.refresh();
        limpiarFormulario();
        actualizarConteo();
        mostrarMensaje("Proveedor guardado.", false);
    }

    @FXML
    private void eliminarProveedor() {
        Proveedor seleccionado = tablaProveedores.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un proveedor de la tabla para eliminarlo.", true);
            return;
        }
        boolean tieneInsumos = InsumosSessionStore.obtenerTodos().stream()
                .anyMatch(insumo -> insumo.getProveedor() != null
                        && insumo.getProveedor().getIdProveedor().equals(seleccionado.getIdProveedor()));
        if (tieneInsumos) {
            mostrarMensaje("No se puede eliminar: hay insumos asignados a este proveedor. Cambia el proveedor de esos insumos primero.", true);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar proveedor");
        confirmacion.setHeaderText("¿Eliminar " + seleccionado.getNombres() + "?");
        confirmacion.setContentText("Se eliminará el proveedor seleccionado.");
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                proveedorRepository.deleteById(seleccionado.getIdProveedor());
                ProveedorSessionStore.recargar();
            } catch (RuntimeException e) {
                mostrarMensaje(e.getMessage(), true);
                return;
            }
            limpiarFormulario();
            actualizarConteo();
            mostrarMensaje("Proveedor eliminado.", false);
        }
    }

    @FXML
    private void limpiarFormulario() {
        idEnEdicion = null;
        cmbTipoDocumento.setValue(TipoDocumento.RUC);
        cmbTipoDocumento.setDisable(false);
        txtDocumento.clear();
        txtNombres.clear();
        txtCelular.clear();
        txtCorreo.clear();
        txtDireccion.clear();
        tablaProveedores.getSelectionModel().clearSelection();
        btnGuardar.setText("Agregar proveedor");
        lblMensaje.setText("");
    }

    private void cargarEnFormulario(Proveedor proveedor) {
        idEnEdicion = proveedor.getIdProveedor();
        cmbTipoDocumento.setValue(proveedor.getTipoDocumento());
        txtDocumento.setText(proveedor.getNumeroDocumento());
        txtNombres.setText(proveedor.getNombres());
        txtCelular.setText(proveedor.getCelular());
        txtCorreo.setText(proveedor.getCorreo());
        txtDireccion.setText(proveedor.getDireccion());
        btnGuardar.setText("Actualizar proveedor");
        mostrarMensaje("Editando proveedor. Sus insumos relacionados se conservan.", false);
    }

    private void filtrar(String consulta) {
        String filtro = consulta == null ? "" : consulta.trim().toLowerCase();
        proveedores.setPredicate(proveedor -> filtro.isEmpty()
                || contiene(proveedor.getNombres(), filtro)
                || contiene(proveedor.getNumeroDocumento(), filtro)
                || contiene(proveedor.getCorreo(), filtro)
                || contiene(proveedor.getCelular(), filtro));
        actualizarConteo();
    }

    private void actualizarConteo() {
        lblConteo.setText(proveedores.size() + " proveedor(es)");
    }

    private static boolean documentoValido(TipoDocumento tipo, String documento) {
        return switch (tipo) {
            case DNI -> documento.matches("\\d{8}");
            case RUC -> documento.matches("\\d{11}");
            case CE, PASAPORTE -> documento.matches("[A-Za-z0-9-]{6,15}");
        };
    }

    private static String tipo(Proveedor proveedor) {
        return proveedor.getTipoDocumento() == null ? "—" : proveedor.getTipoDocumento().name();
    }

    private static String limpio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static boolean contiene(String texto, String filtro) {
        return texto != null && texto.toLowerCase().contains(filtro);
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b3261e;" : "-fx-text-fill: #26734d;");
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }
}


