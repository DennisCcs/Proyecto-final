package pe.edu.upeu.sysvstarbucks.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pe.edu.upeu.sysvstarbucks.SysVentas;
import pe.edu.upeu.sysvstarbucks.enums.EstadoUsuario;
import pe.edu.upeu.sysvstarbucks.model.Usuario;
import pe.edu.upeu.sysvstarbucks.repository.UsuarioRepository;
import pe.edu.upeu.sysvstarbucks.service.SesionUsuario;
import pe.edu.upeu.sysvstarbucks.service.UsuarioSessionStore;
import pe.edu.upeu.sysvstarbucks.utils.ClaveSegura;
import pe.edu.upeu.sysvstarbucks.utils.PreferenciasUI;

import java.io.IOException;
import java.util.Properties;

public class LoginController {
    @FXML private StackPane root;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtClave;
    @FXML private Label lblUsuario;
    @FXML private Label lblClave;
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private Label lblMensaje;
    @FXML private Label lblSalirPregunta;
    @FXML private Hyperlink linkSalir;
    @FXML private Button btnIngresar;

    private Properties textos;
    private final UsuarioRepository usuarioRepository = new UsuarioRepository();

    @FXML
    private void initialize() {
        try {
            usuarioRepository.asegurarAdministradorInicial();
            UsuarioSessionStore.recargar();
        } catch (RuntimeException e) {
            mostrarMensaje("No se pudieron cargar las cuentas guardadas: " + e.getMessage(), true);
        }
        textos = PreferenciasUI.cargarIdioma();
        lblTitulo.setText(texto("login.titulo"));
        lblSubtitulo.setText(texto("login.subtitulo"));
        lblUsuario.setText(texto("login.usuario"));
        lblClave.setText(texto("login.contrasena"));
        txtUsuario.setPromptText(texto("login.prompt.usuario"));
        txtClave.setPromptText(texto("login.prompt.contrasena"));
        btnIngresar.setText(texto("login.boton"));
        lblSalirPregunta.setText(texto("login.salirPregunta"));
        linkSalir.setText(texto("login.salir"));
    }

    @FXML
    private void iniciarSesion() {
        String nombreUsuario = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        String clave = txtClave.getText() == null ? "" : txtClave.getText();
        if (nombreUsuario.isEmpty() || clave.isEmpty()) {
            mostrarMensaje(texto("login.error.campos"), true);
            return;
        }

        Usuario usuario = UsuarioSessionStore.obtenerTodos().stream()
                .filter(cuenta -> cuenta.getUsuario().equalsIgnoreCase(nombreUsuario))
                .findFirst().orElse(null);
        if (usuario == null || !ClaveSegura.verificar(clave, usuario.getClaveHash())) {
            txtClave.clear();
            mostrarMensaje(texto("login.error.credenciales"), true);
            return;
        }
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            txtClave.clear();
            mostrarMensaje(texto("login.error.inactivo"), true);
            return;
        }
        abrirPanel(usuario);
    }

    private void abrirPanel(Usuario usuario) {
        try {
            SesionUsuario.iniciar(usuario);
            FXMLLoader loader = new FXMLLoader(SysVentas.class.getResource("/view/maingui.fxml"));
            Scene escena = new Scene(loader.load(), 1100, 720);
            PreferenciasUI.aplicarTema(escena);
            Stage escenario = (Stage) root.getScene().getWindow();
            escenario.setTitle("Starbucks - Sistema de ventas");
            escenario.setScene(escena);
            escenario.setMinWidth(900);
            escenario.setMinHeight(600);
        } catch (IOException | RuntimeException e) {
            SesionUsuario.cerrar();
            mostrarError(texto("login.error.panel").replace("{0}", String.valueOf(e.getMessage())));
        }
    }

    @FXML
    private void salir() {
        javafx.application.Platform.exit();
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b3261e;" : "-fx-text-fill: #26734d;");
    }

    private String texto(String clave) {
        return textos.getProperty(clave, clave);
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error al iniciar");
        alerta.setHeaderText("No se pudo abrir el sistema");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
