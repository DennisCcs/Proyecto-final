package pe.edu.upeu.sysvstarbucks.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.Scene;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import pe.edu.upeu.sysvstarbucks.SysVentas;
import pe.edu.upeu.sysvstarbucks.model.Usuario;
import pe.edu.upeu.sysvstarbucks.utils.PreferenciasUI;
import pe.edu.upeu.sysvstarbucks.service.SesionUsuario;

import java.io.IOException;
import java.util.Properties;

public class MainguiController {
    @FXML private BorderPane root;
    @FXML private Label lblSesion;
    @FXML private Label lblBienvenida;
    @FXML private Label lblSubtituloApp;
    @FXML private Label lblInstruccionInicio;
    @FXML private Button btnGestionarProductos;
    @FXML private Button btnCerrarSesion;
    @FXML private Menu menuInicio;
    @FXML private Menu menuVentas;
    @FXML private Menu menuCompras;
    @FXML private Menu menuCatalogo;
    @FXML private Menu menuAdministracion;
    @FXML private Menu menuTema;
    @FXML private Menu menuIdioma;
    @FXML private Menu menuSalir;
    @FXML private MenuItem itemInicio;
    @FXML private MenuItem itemNuevaVenta;
    @FXML private MenuItem itemHistorialVentas;
    @FXML private MenuItem itemNuevaCompra;
    @FXML private MenuItem itemHistorialCompras;
    @FXML private MenuItem itemInsumos;
    @FXML private MenuItem itemProductos;
    @FXML private MenuItem itemPresentaciones;
    @FXML private MenuItem itemClientes;
    @FXML private MenuItem itemProveedores;
    @FXML private MenuItem itemUsuarios;
    @FXML private MenuItem itemCerrarAplicacion;

    private Node panelInicio;
    private Properties textos;

    @FXML
    private void initialize() {
        panelInicio = root.getCenter();
        Usuario usuario = SesionUsuario.obtenerUsuarioActual();
        boolean administrador = usuario != null && usuario.getPerfil() != null
                && "ADMINISTRADOR".equals(usuario.getPerfil().getCodigo());
        if (usuario != null) {
            lblSesion.setText(usuario.getUsuario() + " · " + usuario.getPerfil().getNombre());
            lblBienvenida.setText("Bienvenido, " + usuario.getUsuario());
        }
        menuCompras.setVisible(administrador);
        menuCatalogo.setVisible(administrador);
        menuAdministracion.setVisible(administrador);
        btnGestionarProductos.setVisible(administrador);
        btnGestionarProductos.setManaged(administrador);
        textos = PreferenciasUI.cargarIdioma();
        configurarMenuIdioma();
        configurarMenuTema();
        actualizarTextos();
    }

    @FXML
    private void mostrarInicio() {
        root.setCenter(panelInicio);
    }

    @FXML
    private void mostrarProductos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_producto.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir la gestión de productos: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarPresentacionesPendiente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_presentacion.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir la gestión de personalizaciones: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarInsumosPendiente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_insumo.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir la gestión de insumos: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarVentasPendiente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_venta.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir el módulo de ventas: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarHistorialVentas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_historial_ventas.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir el historial de ventas: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarComprasPendiente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_compra.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir el módulo de compras: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarHistorialCompras() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_historial_compras.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir el historial de compras: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarClientesPendiente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_cliente.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir la gestión de clientes: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarProveedoresPendiente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_proveedor.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir la gestión de proveedores: " + e.getMessage());
        }
    }

    @FXML
    private void mostrarUsuariosPendiente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_usuario.fxml"));
            root.setCenter(loader.load());
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo abrir la gestión de usuarios: " + e.getMessage());
        }
    }

    @FXML
    private void salir() {
        Platform.exit();
    }

    @FXML
    private void cerrarSesion() {
        SesionUsuario.cerrar();
        try {
            FXMLLoader loader = new FXMLLoader(SysVentas.class.getResource("/view/main_login.fxml"));
            Scene escena = new Scene(loader.load(), 960, 620);
            PreferenciasUI.aplicarTema(escena);
            Stage escenario = (Stage) root.getScene().getWindow();
            escenario.setTitle("Iniciar sesión - Starbucks");
            escenario.setScene(escena);
            escenario.setMinWidth(820);
            escenario.setMinHeight(560);
        } catch (IOException | RuntimeException e) {
            mostrarError("No se pudo volver al inicio de sesión: " + e.getMessage());
        }
    }

    private void configurarMenuIdioma() {
        menuIdioma.getItems().clear();
        ToggleGroup grupo = new ToggleGroup();
        agregarIdioma(grupo, "es", "idioma.espanol");
        agregarIdioma(grupo, "en", "idioma.ingles");
        agregarIdioma(grupo, "fr", "idioma.frances");
    }

    private void agregarIdioma(ToggleGroup grupo, String codigo, String claveTexto) {
        RadioMenuItem item = new RadioMenuItem(texto(claveTexto));
        item.setToggleGroup(grupo);
        item.setSelected(codigo.equals(PreferenciasUI.idiomaActual()));
        item.setOnAction(event -> cambiarIdioma(codigo));
        menuIdioma.getItems().add(item);
    }

    private void cambiarIdioma(String codigo) {
        PreferenciasUI.guardarIdioma(codigo);
        textos = PreferenciasUI.cargarIdioma();
        actualizarTextos();
        configurarMenuIdioma();
        configurarMenuTema();
    }

    private void configurarMenuTema() {
        menuTema.getItems().clear();
        ToggleGroup grupo = new ToggleGroup();
        agregarTema(grupo, "verde", "tema.verde");
        agregarTema(grupo, "oscuro", "tema.oscuro");
        agregarTema(grupo, "azul", "tema.azul");
        agregarTema(grupo, "rosado", "tema.rosado");
        agregarTema(grupo, "profesional", "tema.profesional");
        agregarTema(grupo, "epico", "tema.epico");
    }

    private void agregarTema(ToggleGroup grupo, String codigo, String claveTexto) {
        RadioMenuItem item = new RadioMenuItem(texto(claveTexto));
        item.setToggleGroup(grupo);
        item.setSelected(codigo.equals(PreferenciasUI.temaActual()));
        item.setOnAction(event -> {
            PreferenciasUI.guardarTema(codigo);
            PreferenciasUI.aplicarTema(root.getScene());
        });
        menuTema.getItems().add(item);
    }

    private void actualizarTextos() {
        menuInicio.setText(texto("menu.nombre.principal"));
        menuVentas.setText(texto("menu.nombre.venta"));
        menuCompras.setText(texto("menu.nombre.compra"));
        menuCatalogo.setText(texto("menu.nombre.producto"));
        menuAdministracion.setText(texto("menu.nombre.administracion"));
        menuTema.setText(texto("menu.nombre.tema"));
        menuIdioma.setText(texto("menu.nombre.idioma"));
        menuSalir.setText(texto("menu.nombre.salir"));

        itemInicio.setText(texto("menuitem.nombre.inicio"));
        itemNuevaVenta.setText(texto("menuitem.nombre.nuevaVenta"));
        itemHistorialVentas.setText(texto("menuitem.nombre.historialVentas"));
        itemNuevaCompra.setText(texto("menuitem.nombre.nuevaCompra"));
        itemHistorialCompras.setText(texto("menuitem.nombre.historialCompras"));
        itemInsumos.setText(texto("menuitem.nombre.insumos"));
        itemProductos.setText(texto("menuitem.nombre.producto"));
        itemPresentaciones.setText(texto("menuitem.nombre.presentaciones"));
        itemClientes.setText(texto("menuitem.nombre.clientes"));
        itemProveedores.setText(texto("menuitem.nombre.proveedores"));
        itemUsuarios.setText(texto("menuitem.nombre.usuarios"));
        itemCerrarAplicacion.setText(texto("menuitem.nombre.cerrarAplicacion"));
        btnCerrarSesion.setText(texto("menuitem.nombre.cerrarSesion"));
        lblSubtituloApp.setText(texto("header.subtitulo"));
        lblInstruccionInicio.setText(texto("home.instrucciones"));
        btnGestionarProductos.setText(texto("home.gestionarProductos"));

        Usuario usuario = SesionUsuario.obtenerUsuarioActual();
        if (usuario != null) {
            String clavePerfil = usuario.getPerfil() != null
                    && "ADMINISTRADOR".equals(usuario.getPerfil().getCodigo())
                    ? "perfil.administrador" : "perfil.vendedor";
            lblSesion.setText(usuario.getUsuario() + " · " + texto(clavePerfil));
            lblBienvenida.setText(texto("home.bienvenida").replace("{0}", usuario.getUsuario()));
        }
    }

    private String texto(String clave) {
        return textos.getProperty(clave, clave);
    }

    private void mostrarPendiente(String nombreModulo) {
        Label titulo = new Label(nombreModulo);
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e3932;");

        Label mensaje = new Label("Esta pantalla se implementará en la siguiente etapa del proyecto.");
        mensaje.setStyle("-fx-font-size: 14px; -fx-text-fill: #5b625f;");

        VBox contenido = new VBox(12, titulo, mensaje);
        contenido.setStyle("-fx-padding: 36px; -fx-background-color: #f5f4f0;");
        root.setCenter(contenido);
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error al abrir la pantalla");
        alerta.setHeaderText("No se pudo cargar el módulo");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
