package pe.edu.upeu.sysvstarbucks.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import pe.edu.upeu.sysvstarbucks.enums.EstadoUsuario;
import pe.edu.upeu.sysvstarbucks.model.Perfil;
import pe.edu.upeu.sysvstarbucks.model.Usuario;
import pe.edu.upeu.sysvstarbucks.repository.UsuarioRepository;
import pe.edu.upeu.sysvstarbucks.service.UsuarioSessionStore;
import pe.edu.upeu.sysvstarbucks.utils.ClaveSegura;

import java.util.Optional;

public class UsuarioController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtClave;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Perfil> cmbPerfil;
    @FXML private ComboBox<EstadoUsuario> cmbEstado;
    @FXML private Button btnGuardar;
    @FXML private Label lblMensaje;
    @FXML private Label lblConteo;
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colId;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colPerfil;
    @FXML private TableColumn<Usuario, String> colEstado;

    private final FilteredList<Usuario> usuarios = new FilteredList<>(UsuarioSessionStore.obtenerTodos(), usuario -> true);
    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private Long idEnEdicion;

    @FXML
    private void initialize() {
        try {
            UsuarioSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
        cmbPerfil.setItems(UsuarioSessionStore.obtenerPerfiles());
        cmbPerfil.setConverter(new StringConverter<>() {
            @Override public String toString(Perfil perfil) {
                return perfil == null ? "" : perfil.getNombre();
            }
            @Override public Perfil fromString(String texto) { return null; }
        });
        cmbEstado.getItems().setAll(EstadoUsuario.values());
        cmbEstado.setConverter(new StringConverter<>() {
            @Override public String toString(EstadoUsuario estado) {
                if (estado == null) return "";
                return estado == EstadoUsuario.ACTIVO ? "Activo" : "Inactivo";
            }
            @Override public EstadoUsuario fromString(String texto) { return null; }
        });
        cmbEstado.setValue(EstadoUsuario.ACTIVO);

        colId.setCellValueFactory(cell -> texto(String.valueOf(cell.getValue().getIdUsuario())));
        colUsuario.setCellValueFactory(cell -> texto(cell.getValue().getUsuario()));
        colPerfil.setCellValueFactory(cell -> texto(nombrePerfil(cell.getValue())));
        colEstado.setCellValueFactory(cell -> texto(nombreEstado(cell.getValue().getEstado())));
        tablaUsuarios.setItems(usuarios);
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, anterior, usuario) -> {
            if (usuario != null) cargarEnFormulario(usuario);
        });
        txtBuscar.textProperty().addListener((obs, anterior, valor) -> filtrar(valor));
        actualizarConteo();
    }

    @FXML
    private void guardarUsuario() {
        String nombreUsuario = limpio(txtUsuario.getText());
        String clave = txtClave.getText() == null ? "" : txtClave.getText();
        Perfil perfil = cmbPerfil.getValue();
        EstadoUsuario estado = cmbEstado.getValue();

        if (nombreUsuario.isEmpty() || perfil == null || estado == null) {
            mostrarMensaje("Completa el usuario, el perfil y el estado.", true);
            return;
        }
        if (idEnEdicion == null && clave.isBlank()) {
            mostrarMensaje("Ingresa una contraseña para el nuevo usuario.", true);
            return;
        }
        if (!clave.isEmpty() && clave.length() < 8) {
            mostrarMensaje("La contraseña debe tener al menos 8 caracteres.", true);
            return;
        }
        boolean usuarioDuplicado = UsuarioSessionStore.obtenerTodos().stream()
                .anyMatch(usuario -> usuario.getUsuario().equalsIgnoreCase(nombreUsuario)
                        && !usuario.getIdUsuario().equals(idEnEdicion));
        if (usuarioDuplicado) {
            mostrarMensaje("Ese nombre de usuario ya está registrado.", true);
            return;
        }

        if (idEnEdicion != null) {
            Usuario existente = UsuarioSessionStore.buscarPorId(idEnEdicion);
            if (esUltimoAdministradorActivo(existente)
                    && (!"ADMINISTRADOR".equals(perfil.getCodigo()) || estado != EstadoUsuario.ACTIVO)) {
                mostrarMensaje("No puedes desactivar o cambiar el perfil del último administrador activo.", true);
                return;
            }
        }

        boolean esNuevo = idEnEdicion == null;
        Usuario usuario;
        if (esNuevo) {
            usuario = new Usuario();
        } else {
            usuario = UsuarioSessionStore.buscarPorId(idEnEdicion);
            if (usuario == null) {
                mostrarMensaje("No se encontró el usuario que se quiere actualizar.", true);
                return;
            }
        }
        usuario.setUsuario(nombreUsuario);
        usuario.setPerfil(perfil);
        usuario.setEstado(estado);
        if (!clave.isEmpty()) usuario.setClaveHash(ClaveSegura.hash(clave));

        try {
            if (esNuevo) usuarioRepository.save(usuario);
            else usuarioRepository.update(usuario);
            UsuarioSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
            return;
        }

        tablaUsuarios.refresh();
        limpiarFormulario();
        actualizarConteo();
        mostrarMensaje(esNuevo ? "Usuario agregado." : "Usuario actualizado.", false);
    }

    @FXML
    private void eliminarUsuario() {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un usuario de la tabla para eliminarlo.", true);
            return;
        }
        if (esUltimoAdministradorActivo(seleccionado)) {
            mostrarMensaje("No puedes eliminar al último administrador activo.", true);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar usuario");
        confirmacion.setHeaderText("¿Eliminar el usuario " + seleccionado.getUsuario() + "?");
        confirmacion.setContentText("Se eliminará el usuario seleccionado.");
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                usuarioRepository.deleteById(seleccionado.getIdUsuario());
                UsuarioSessionStore.recargar();
            } catch (RuntimeException e) {
                mostrarMensaje(e.getMessage(), true);
                return;
            }
            limpiarFormulario();
            actualizarConteo();
            mostrarMensaje("Usuario eliminado.", false);
        }
    }

    @FXML
    private void limpiarFormulario() {
        idEnEdicion = null;
        txtUsuario.clear();
        txtClave.clear();
        cmbPerfil.getSelectionModel().clearSelection();
        cmbEstado.setValue(EstadoUsuario.ACTIVO);
        tablaUsuarios.getSelectionModel().clearSelection();
        btnGuardar.setText("Agregar usuario");
        lblMensaje.setText("");
    }

    private void cargarEnFormulario(Usuario usuario) {
        idEnEdicion = usuario.getIdUsuario();
        txtUsuario.setText(usuario.getUsuario());
        txtClave.clear();
        cmbPerfil.setValue(usuario.getPerfil());
        cmbEstado.setValue(usuario.getEstado());
        btnGuardar.setText("Guardar cambios");
        mostrarMensaje("Contraseña oculta. Déjala vacía para conservarla o ingresa otra para cambiarla.", false);
    }

    private void filtrar(String consulta) {
        String filtro = consulta == null ? "" : consulta.trim().toLowerCase();
        usuarios.setPredicate(usuario -> filtro.isEmpty()
                || contiene(usuario.getUsuario(), filtro)
                || contiene(nombrePerfil(usuario), filtro)
                || contiene(nombreEstado(usuario.getEstado()), filtro));
        actualizarConteo();
    }

    private void actualizarConteo() {
        lblConteo.setText(usuarios.size() + " usuario(s)");
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b3261e;" : "-fx-text-fill: #26734d;");
    }

    private static String nombrePerfil(Usuario usuario) {
        return usuario.getPerfil() == null ? "" : usuario.getPerfil().getNombre();
    }

    private static String nombreEstado(EstadoUsuario estado) {
        if (estado == null) return "";
        return estado == EstadoUsuario.ACTIVO ? "Activo" : "Inactivo";
    }

    private static boolean esUltimoAdministradorActivo(Usuario usuario) {
        if (usuario == null || usuario.getEstado() != EstadoUsuario.ACTIVO
                || usuario.getPerfil() == null
                || !"ADMINISTRADOR".equals(usuario.getPerfil().getCodigo())) return false;
        long otrosAdministradoresActivos = UsuarioSessionStore.obtenerTodos().stream()
                .filter(otro -> !otro.getIdUsuario().equals(usuario.getIdUsuario()))
                .filter(otro -> otro.getEstado() == EstadoUsuario.ACTIVO)
                .filter(otro -> otro.getPerfil() != null
                        && "ADMINISTRADOR".equals(otro.getPerfil().getCodigo()))
                .count();
        return otrosAdministradoresActivos == 0;
    }

    private static String limpio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase().contains(filtro);
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }
}

