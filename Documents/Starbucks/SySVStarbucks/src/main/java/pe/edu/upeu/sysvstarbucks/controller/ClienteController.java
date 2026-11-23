package pe.edu.upeu.sysvstarbucks.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
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
import pe.edu.upeu.sysvstarbucks.dto.PersonaDni;
import pe.edu.upeu.sysvstarbucks.enums.TipoDocumento;
import pe.edu.upeu.sysvstarbucks.model.Cliente;
import pe.edu.upeu.sysvstarbucks.repository.ClienteRepository;
import pe.edu.upeu.sysvstarbucks.service.ClienteSessionStore;
import pe.edu.upeu.sysvstarbucks.utils.ConsultaDniService;

import java.util.Optional;

public class ClienteController {
    @FXML private ComboBox<TipoDocumento> cmbTipoDocumento;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtNombres;
    @FXML private TextField txtBuscar;
    @FXML private TextArea txtDireccion;
    @FXML private Button btnConsultar;
    @FXML private Button btnGuardar;
    @FXML private Label lblMensaje;
    @FXML private Label lblConteo;
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colTipo;
    @FXML private TableColumn<Cliente, String> colDocumento;
    @FXML private TableColumn<Cliente, String> colNombres;
    @FXML private TableColumn<Cliente, String> colDireccion;

    private final FilteredList<Cliente> clientes = new FilteredList<>(ClienteSessionStore.obtenerTodos(), cliente -> true);
    private final ClienteRepository clienteRepository = new ClienteRepository();
    private final ConsultaDniService consultaDniService = new ConsultaDniService();
    private String documentoEnEdicion;

    @FXML
    private void initialize() {
        try {
            ClienteSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
        cmbTipoDocumento.setItems(FXCollections.observableArrayList(TipoDocumento.values()));
        cmbTipoDocumento.setValue(TipoDocumento.DNI);
        cmbTipoDocumento.setConverter(new StringConverter<>() {
            @Override public String toString(TipoDocumento tipo) { return tipo == null ? "" : tipo.name(); }
            @Override public TipoDocumento fromString(String texto) {
                try { return TipoDocumento.valueOf(texto); }
                catch (IllegalArgumentException e) { return null; }
            }
        });

        colTipo.setCellValueFactory(cell -> texto(cell.getValue().getTipoDocumento().name()));
        colDocumento.setCellValueFactory(cell -> texto(cell.getValue().getNumeroDocumento()));
        colNombres.setCellValueFactory(cell -> texto(cell.getValue().getNombres()));
        colDireccion.setCellValueFactory(cell -> texto(cell.getValue().getDireccion()));
        tablaClientes.setItems(clientes);
        tablaClientes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) cargarEnFormulario(seleccionado);
        });
        txtBuscar.textProperty().addListener((obs, anterior, valor) -> filtrar(valor));
        actualizarConteo();
    }

    @FXML
    private void consultarDocumento() {
        TipoDocumento tipo = cmbTipoDocumento.getValue();
        String documento = txtDocumento.getText() == null ? "" : txtDocumento.getText().trim();
        if (tipo == null || documento.isEmpty()) {
            mostrarMensaje("Selecciona el tipo e ingresa el número de documento.", true);
            return;
        }

        Cliente clienteGuardado = ClienteSessionStore.buscar(documento);
        if (clienteGuardado != null) {
            if (clienteGuardado.getTipoDocumento() != tipo) {
                mostrarMensaje("Ese número ya está registrado con otro tipo de documento.", true);
                return;
            }
            cargarEnFormulario(clienteGuardado);
            mostrarMensaje("Cliente encontrado en los registros; se cargó también su dirección guardada.", false);
            return;
        }

        if (tipo != TipoDocumento.DNI) {
            mostrarMensaje("La consulta en línea del ejemplo funciona para DNI. Completa manualmente los datos de este documento.", false);
            return;
        }
        if (!documento.matches("\\d{8}")) {
            mostrarMensaje("El DNI debe tener exactamente 8 dígitos.", true);
            return;
        }

        btnConsultar.setDisable(true);
        txtDocumento.setDisable(true);
        mostrarMensaje("Consultando nombres y apellidos…", false);
        Task<Optional<PersonaDni>> tarea = new Task<>() {
            @Override protected Optional<PersonaDni> call() throws Exception {
                return consultaDniService.consultar(documento);
            }
        };
        tarea.setOnSucceeded(evento -> {
            btnConsultar.setDisable(false);
            txtDocumento.setDisable(false);
            Optional<PersonaDni> resultado = tarea.getValue();
            if (resultado.isPresent()) {
                PersonaDni persona = resultado.get();
                txtDocumento.setText(persona.dni());
                txtNombres.setText(persona.nombreCompleto());
                txtDireccion.clear();
                mostrarMensaje("Datos encontrados. Revisa el nombre y escribe la dirección manualmente.", false);
            } else {
                mostrarMensaje("No se encontraron datos. Puedes ingresar el nombre y guardar el cliente manualmente.", true);
            }
        });
        tarea.setOnFailed(evento -> {
            btnConsultar.setDisable(false);
            txtDocumento.setDisable(false);
            mostrarMensaje("No se pudo consultar la página. Puedes ingresar los datos manualmente.", true);
        });
        Thread hilo = new Thread(tarea, "consulta-dni-cliente");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void guardarCliente() {
        TipoDocumento tipo = cmbTipoDocumento.getValue();
        String documento = txtDocumento.getText() == null ? "" : txtDocumento.getText().trim();
        String nombres = txtNombres.getText() == null ? "" : txtNombres.getText().trim();
        String direccion = txtDireccion.getText() == null ? "" : txtDireccion.getText().trim();
        if (tipo == null || documento.isEmpty() || nombres.isEmpty()) {
            mostrarMensaje("El tipo, número de documento y nombre son obligatorios.", true);
            return;
        }
        if (tipo == TipoDocumento.DNI && !documento.matches("\\d{8}")) {
            mostrarMensaje("El DNI debe tener exactamente 8 dígitos.", true);
            return;
        }
        if (tipo == TipoDocumento.RUC && !documento.matches("\\d{11}")) {
            mostrarMensaje("El RUC debe tener exactamente 11 dígitos.", true);
            return;
        }

        Cliente existente = ClienteSessionStore.buscar(documento);
        if (documentoEnEdicion == null && existente != null) {
            mostrarMensaje("Ese documento ya está registrado. Consúltalo para cargar y editar el cliente.", true);
            return;
        }

        try {
            Cliente cliente = documentoEnEdicion == null ? new Cliente()
                    : ClienteSessionStore.buscar(documentoEnEdicion);
            if (cliente == null) {
                mostrarMensaje("No se encontró el cliente que se quiere actualizar.", true);
                return;
            }
            cliente.setNumeroDocumento(documento);
            cliente.setTipoDocumento(tipo);
            cliente.setNombres(nombres);
            cliente.setDireccion(direccion.isBlank() ? null : direccion);
            if (documentoEnEdicion == null) clienteRepository.save(cliente);
            else clienteRepository.update(cliente, documentoEnEdicion);
            ClienteSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
            return;
        }
        tablaClientes.refresh();
        limpiarFormulario();
        actualizarConteo();
        mostrarMensaje("Cliente guardado.", false);
    }

    @FXML
    private void eliminarCliente() {
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un cliente de la tabla para eliminarlo.", true);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar cliente");
        confirmacion.setHeaderText("¿Eliminar " + seleccionado.getNombres() + "?");
        confirmacion.setContentText("Se eliminará el cliente seleccionado.");
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                clienteRepository.deleteByDocumento(seleccionado.getNumeroDocumento());
                ClienteSessionStore.recargar();
            } catch (RuntimeException e) {
                mostrarMensaje(e.getMessage(), true);
                return;
            }
            limpiarFormulario();
            actualizarConteo();
            mostrarMensaje("Cliente eliminado.", false);
        }
    }

    @FXML
    private void limpiarFormulario() {
        documentoEnEdicion = null;
        cmbTipoDocumento.setValue(TipoDocumento.DNI);
        cmbTipoDocumento.setDisable(false);
        txtDocumento.clear();
        txtDocumento.setDisable(false);
        txtNombres.clear();
        txtDireccion.clear();
        btnConsultar.setDisable(false);
        btnGuardar.setText("Guardar cliente");
        tablaClientes.getSelectionModel().clearSelection();
        lblMensaje.setText("");
    }

    private void cargarEnFormulario(Cliente cliente) {
        documentoEnEdicion = cliente.getNumeroDocumento();
        cmbTipoDocumento.setValue(cliente.getTipoDocumento());
        cmbTipoDocumento.setDisable(true);
        txtDocumento.setText(cliente.getNumeroDocumento());
        txtDocumento.setDisable(true);
        txtNombres.setText(cliente.getNombres());
        txtDireccion.setText(cliente.getDireccion());
        btnConsultar.setDisable(true);
        btnGuardar.setText("Actualizar cliente");
        mostrarMensaje("Cliente seleccionado. Su dirección proviene de los datos guardados.", false);
    }

    private void filtrar(String consulta) {
        String filtro = consulta == null ? "" : consulta.trim().toLowerCase();
        clientes.setPredicate(cliente -> filtro.isEmpty()
                || cliente.getNumeroDocumento().toLowerCase().contains(filtro)
                || cliente.getNombres().toLowerCase().contains(filtro));
        actualizarConteo();
    }

    private void actualizarConteo() {
        lblConteo.setText(clientes.size() + " cliente(s)");
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b3261e;" : "-fx-text-fill: #26734d;");
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }
}

