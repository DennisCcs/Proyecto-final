package pe.edu.upeu.sysvstarbucks.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pe.edu.upeu.sysvstarbucks.dto.comprobante.BoletaVenta;
import pe.edu.upeu.sysvstarbucks.dto.comprobante.ItemBoleta;
import pe.edu.upeu.sysvstarbucks.model.Cliente;
import pe.edu.upeu.sysvstarbucks.model.Presentacion;
import pe.edu.upeu.sysvstarbucks.model.Producto;
import pe.edu.upeu.sysvstarbucks.model.Usuario;
import pe.edu.upeu.sysvstarbucks.repository.PresentacionRepository;
import pe.edu.upeu.sysvstarbucks.repository.ProductoRepository;
import pe.edu.upeu.sysvstarbucks.repository.VentaRepository;
import pe.edu.upeu.sysvstarbucks.service.ClienteSessionStore;
import pe.edu.upeu.sysvstarbucks.service.SesionUsuario;
import pe.edu.upeu.sysvstarbucks.service.VentaSessionStore;
import pe.edu.upeu.sysvstarbucks.service.VentaSessionStore.DetalleVentaGuardado;
import pe.edu.upeu.sysvstarbucks.service.VentaSessionStore.VentaRegistrada;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class VentaController {
    private static final BigDecimal IGV = new BigDecimal("0.18");

    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private ComboBox<OpcionVenta> cmbPresentacion;
    @FXML private ComboBox<String> cmbOpcion1;
    @FXML private Label lblOpcion1;
    @FXML private ListView<Presentacion> lstPersonalizaciones;
    @FXML private Label lblPrecio;
    @FXML private Label lblMensaje;
    @FXML private Label lblNeto;
    @FXML private Label lblIgv;
    @FXML private Label lblTotal;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtDocumentoCliente;
    @FXML private TextField txtNombreCliente;
    @FXML private ListView<Cliente> lstClientesSugeridos;
    @FXML private TableView<LineaCarrito> tablaCarrito;
    @FXML private TableColumn<LineaCarrito, String> colProducto;
    @FXML private TableColumn<LineaCarrito, String> colDetalle;
    @FXML private TableColumn<LineaCarrito, Integer> colCantidad;
    @FXML private TableColumn<LineaCarrito, String> colPrecio;
    @FXML private TableColumn<LineaCarrito, String> colSubtotal;

    private final ProductoRepository productoRepository = new ProductoRepository();
    private final PresentacionRepository presentacionRepository = new PresentacionRepository();
    private final VentaRepository ventaRepository = new VentaRepository();
    private final ObservableList<LineaCarrito> lineas = FXCollections.observableArrayList();
    private final ObservableList<Cliente> sugerenciasClientes = FXCollections.observableArrayList();
    private final Set<Long> personalizacionesSeleccionadas = new HashSet<>();
    private Cliente clienteSeleccionado;
    private boolean actualizandoCliente;

    @FXML
    private void initialize() {
        ClienteSessionStore.recargar();
        configurarTabla();
        cmbProducto.setItems(FXCollections.observableArrayList(productoRepository.findAll()));
        cmbProducto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Producto producto) {
                return producto == null ? "" : producto.getNombre() + " — S/ " + dinero(producto.getPrecioBase());
            }
            @Override public Producto fromString(String texto) { return null; }
        });
        cmbProducto.valueProperty().addListener((obs, anterior, producto) -> configurarOpcionesProducto(producto));
        cmbPresentacion.valueProperty().addListener((obs, anterior, opcion) -> actualizarPrecio());
        cmbOpcion1.valueProperty().addListener((obs, anterior, opcion) -> actualizarPrecio());
        configurarListaPersonalizaciones();
        txtNombreCliente.setEditable(false);
        lstClientesSugeridos.setItems(sugerenciasClientes);
        lstClientesSugeridos.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Cliente cliente, boolean vacia) {
                super.updateItem(cliente, vacia);
                setText(vacia || cliente == null ? null
                        : cliente.getTipoDocumento().name() + " " + cliente.getNumeroDocumento()
                        + " — " + cliente.getNombres());
            }
        });
        lstClientesSugeridos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, cliente) -> {
            if (cliente != null && !actualizandoCliente) seleccionarCliente(cliente);
        });
        txtDocumentoCliente.textProperty().addListener((obs, anterior, texto) -> filtrarClientes(texto));
        ocultarSugerenciasClientes();
        actualizarTotales();
    }

    private void filtrarClientes(String texto) {
        if (actualizandoCliente) return;
        clienteSeleccionado = null;
        txtNombreCliente.clear();
        lstClientesSugeridos.getSelectionModel().clearSelection();
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        if (filtro.isEmpty()) {
            sugerenciasClientes.clear();
            ocultarSugerenciasClientes();
            return;
        }
        boolean buscaDocumento = filtro.chars().allMatch(Character::isDigit);
        List<Cliente> coincidencias = ClienteSessionStore.obtenerTodos().stream()
                .filter(cliente -> {
                    String documento = cliente.getNumeroDocumento() == null
                            ? "" : cliente.getNumeroDocumento().toLowerCase(Locale.ROOT);
                    String nombre = cliente.getNombres() == null
                            ? "" : cliente.getNombres().toLowerCase(Locale.ROOT);
                    return buscaDocumento ? documento.startsWith(filtro)
                            : documento.contains(filtro) || nombre.contains(filtro);
                })
                .limit(8)
                .toList();
        sugerenciasClientes.setAll(coincidencias);
        lstClientesSugeridos.setManaged(true);
        lstClientesSugeridos.setVisible(true);
    }

    private void seleccionarCliente(Cliente cliente) {
        actualizandoCliente = true;
        clienteSeleccionado = cliente;
        txtDocumentoCliente.setText(cliente.getNumeroDocumento());
        txtNombreCliente.setText(cliente.getNombres());
        sugerenciasClientes.clear();
        lstClientesSugeridos.getSelectionModel().clearSelection();
        ocultarSugerenciasClientes();
        actualizandoCliente = false;
        mostrarMensaje("Cliente " + cliente.getNombres() + " seleccionado.", false);
    }

    private void ocultarSugerenciasClientes() {
        lstClientesSugeridos.setVisible(false);
        lstClientesSugeridos.setManaged(false);
    }

    private void configurarTabla() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colDetalle.setCellValueFactory(new PropertyValueFactory<>("detalle"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        tablaCarrito.setItems(lineas);
    }

    private void configurarListaPersonalizaciones() {
        lstPersonalizaciones.setCellFactory(lista -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setWrapText(true);
                checkBox.setOnAction(event -> {
                    Presentacion opcion = getItem();
                    if (opcion == null || opcion.getIdPresentacion() == null) return;
                    if (checkBox.isSelected()) personalizacionesSeleccionadas.add(opcion.getIdPresentacion());
                    else personalizacionesSeleccionadas.remove(opcion.getIdPresentacion());
                    actualizarPrecio();
                });
            }

            @Override
            protected void updateItem(Presentacion opcion, boolean vacia) {
                super.updateItem(opcion, vacia);
                if (vacia || opcion == null) {
                    setGraphic(null);
                    return;
                }
                String unidad = opcion.getUnidadMedida() == null ? ""
                        : " " + opcion.getUnidadMedida().getNombreMedida();
                checkBox.setText(opcion.getNombreOpcion() + " ("
                        + formatearCantidad(opcion.getCantidadMedida()) + unidad + ") + S/ "
                        + dinero(opcion.getIncrementoPrecio()));
                checkBox.setSelected(opcion.getIdPresentacion() != null
                        && personalizacionesSeleccionadas.contains(opcion.getIdPresentacion()));
                setGraphic(checkBox);
            }
        });
    }

    private void configurarOpcionesProducto(Producto producto) {
        cmbPresentacion.getItems().clear();
        cmbOpcion1.getItems().clear();
        cmbOpcion1.getSelectionModel().clearSelection();
        personalizacionesSeleccionadas.clear();
        lstPersonalizaciones.getItems().clear();
        if (producto == null || producto.getMenu() == null) {
            lblOpcion1.setText("Opción");
            mostrarPersonalizaciones(false);
            mostrarOpcionSimple(false);
            return;
        }
        String menu = producto.getMenu().getNombre().toLowerCase();
        if (menu.contains("bebida")) {
            cmbPresentacion.getItems().addAll(
                    new OpcionVenta("Alto (300 ml)", 0),
                    new OpcionVenta("Grande (400 ml)", 1.50),
                    new OpcionVenta("Venti (500 ml)", 2.50));
            lblOpcion1.setText("Personaliza tu producto (opcional)");
            mostrarOpcionSimple(false);
            mostrarPersonalizaciones(true);
            try {
                lstPersonalizaciones.getItems().setAll(presentacionRepository.findByProducto(producto));
            } catch (RuntimeException e) {
                mostrarMensaje(e.getMessage(), true);
            }
        } else if (menu.contains("alimento")) {
            cmbPresentacion.getItems().add(new OpcionVenta("Única", 0));
            lblOpcion1.setText("Temperatura (requerida)");
            mostrarPersonalizaciones(false);
            mostrarOpcionSimple(true);
            cmbOpcion1.getItems().addAll("Caliente", "Sin calentar");
        } else {
            cmbPresentacion.getItems().add(new OpcionVenta("Única", 0));
            lblOpcion1.setText("Molido (opcional)");
            mostrarPersonalizaciones(false);
            mostrarOpcionSimple(true);
            cmbOpcion1.getItems().addAll("Sin moler (en grano)", "Cafetera gota a gota", "Prensa francesa");
        }
        cmbPresentacion.getSelectionModel().selectFirst();
        cmbOpcion1.getSelectionModel().clearSelection();
        actualizarPrecio();
    }

    private void mostrarPersonalizaciones(boolean mostrar) {
        lstPersonalizaciones.setVisible(mostrar);
        lstPersonalizaciones.setManaged(mostrar);
    }

    private void mostrarOpcionSimple(boolean mostrar) {
        cmbOpcion1.setVisible(mostrar);
        cmbOpcion1.setManaged(mostrar);
    }

    private void actualizarPrecio() {
        Producto producto = cmbProducto.getValue();
        OpcionVenta presentacion = cmbPresentacion.getValue();
        if (producto == null) {
            lblPrecio.setText("Precio: S/ 0.00");
            return;
        }
        BigDecimal precio = BigDecimal.valueOf(producto.getPrecioBase());
        if (presentacion != null) precio = precio.add(BigDecimal.valueOf(presentacion.incremento));
        for (Presentacion opcion : personalizacionesActuales()) {
            precio = precio.add(BigDecimal.valueOf(opcion.getIncrementoPrecio()));
        }
        lblPrecio.setText("Precio: S/ " + precio.setScale(2, RoundingMode.HALF_UP));
    }

    private List<Presentacion> personalizacionesActuales() {
        return lstPersonalizaciones.getItems().stream()
                .filter(opcion -> opcion.getIdPresentacion() != null
                        && personalizacionesSeleccionadas.contains(opcion.getIdPresentacion()))
                .toList();
    }

    @FXML
    private void agregarAlCarrito() {
        Producto producto = cmbProducto.getValue();
        OpcionVenta presentacion = cmbPresentacion.getValue();
        if (producto == null || presentacion == null) {
            mostrarMensaje("Selecciona un producto y su presentación.", true);
            return;
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad < 1) throw new NumberFormatException();
        } catch (RuntimeException e) {
            mostrarMensaje("La cantidad debe ser un número entero mayor que cero.", true);
            return;
        }
        if (producto.getMenu().getNombre().equalsIgnoreCase("Alimentos") && cmbOpcion1.getValue() == null) {
            mostrarMensaje("Selecciona la temperatura del alimento.", true);
            return;
        }

        BigDecimal precio = BigDecimal.valueOf(producto.getPrecioBase())
                .add(BigDecimal.valueOf(presentacion.incremento));
        List<Presentacion> personalizaciones = personalizacionesActuales();
        for (Presentacion opcion : personalizaciones) {
            precio = precio.add(BigDecimal.valueOf(opcion.getIncrementoPrecio()));
        }
        List<String> opciones = new ArrayList<>();
        personalizaciones.forEach(opcion -> opciones.add(describirPersonalizacion(opcion)));
        if (cmbOpcion1.isManaged() && cmbOpcion1.getValue() != null) opciones.add(cmbOpcion1.getValue());
        String detalle = presentacion.nombre + (opciones.isEmpty() ? "" : " · " + String.join(" · ", opciones));
        BigDecimal precioAplicado = precio;
        LineaCarrito lineaExistente = lineas.stream()
                .filter(linea -> linea.esMismaLinea(producto.getNombre(), detalle, precioAplicado))
                .findFirst()
                .orElse(null);
        if (lineaExistente == null) {
            lineas.add(new LineaCarrito(producto.getNombre(), detalle, cantidad, precio));
            mostrarMensaje("Producto agregado al carrito.", false);
        } else {
            int indice = lineas.indexOf(lineaExistente);
            lineas.set(indice, lineaExistente.conCantidad(lineaExistente.getCantidad() + cantidad));
            mostrarMensaje("Se sumó la cantidad en la línea existente del carrito.", false);
        }
        txtCantidad.setText("1");
        actualizarTotales();
    }

    @FXML
    private void quitarDelCarrito() {
        LineaCarrito seleccionada = tablaCarrito.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Selecciona una línea del carrito para quitarla.", true);
            return;
        }
        lineas.remove(seleccionada);
        actualizarTotales();
    }

    @FXML
    private void vaciarCarrito() {
        lineas.clear();
        actualizarTotales();
        mostrarMensaje("Carrito vacío.", false);
    }

    @FXML
    private void continuarBoleta() {
        if (lineas.isEmpty()) {
            mostrarMensaje("Agrega al menos un producto antes de continuar.", true);
            return;
        }
        String documento = txtDocumentoCliente.getText() == null ? "" : txtDocumentoCliente.getText().trim();
        if (!documento.isEmpty() && clienteSeleccionado == null) {
            mostrarMensaje("Selecciona al cliente desde la lista de sugerencias o borra el documento.", true);
            return;
        }

        Totales totales = calcularTotales();
        Usuario usuario = SesionUsuario.obtenerUsuarioActual();
        if (usuario == null || usuario.getIdUsuario() == null) {
            mostrarMensaje("No se encontró una sesión de usuario válida para registrar la venta.", true);
            return;
        }
        String vendedor = usuario.getUsuario();
        List<DetalleVentaGuardado> detalles = lineas.stream().map(linea -> new DetalleVentaGuardado(
                linea.getProducto(), linea.getDetalle(), linea.getCantidad(),
                linea.precioDecimal, linea.getSubtotalDecimal())).toList();
        VentaRegistrada venta;
        try {
            venta = ventaRepository.registrarVenta(usuario.getIdUsuario(),
                    clienteSeleccionado == null ? null : clienteSeleccionado.getNombres(),
                    clienteSeleccionado == null ? null : clienteSeleccionado.getTipoDocumento().name(),
                    clienteSeleccionado == null ? null : clienteSeleccionado.getNumeroDocumento(),
                    vendedor, detalles, totales.neto(), totales.igv(), totales.total());
            VentaSessionStore.agregar(venta);
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
            return;
        }

        boolean boletaAbierta = mostrarBoleta(venta, detalles);
        lineas.clear();
        txtDocumentoCliente.clear();
        txtNombreCliente.clear();
        clienteSeleccionado = null;
        txtCantidad.setText("1");
        actualizarTotales();
        if (boletaAbierta) mostrarMensaje("Boleta " + venta.codigo() + " generada.", false);
    }

    private boolean mostrarBoleta(VentaRegistrada venta, List<DetalleVentaGuardado> detalles) {
        List<ItemBoleta> items = detalles.stream()
                .map(item -> new ItemBoleta(item.producto(), item.detalle(), item.cantidad(),
                        item.precioUnitario(), item.subtotal()))
                .toList();
        BoletaVenta boleta = new BoletaVenta(venta.codigo(), LocalDateTime.now(),
                venta.cliente(), venta.tipoDocumento(), venta.documento(),
                clienteSeleccionado == null ? "—" : clienteSeleccionado.getDireccion(),
                items, venta.neto(), venta.igv(), venta.total());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_boleta.fxml"));
            Parent root = loader.load();
            BoletaController controller = loader.getController();
            controller.setBoleta(boleta);

            Stage stage = new Stage();
            stage.initOwner(tablaCarrito.getScene().getWindow());
            stage.setTitle("Boleta " + venta.codigo());
            stage.setScene(new Scene(root, 920, 760));
            stage.setMinWidth(760);
            stage.setMinHeight(600);
            stage.show();
            return true;
        } catch (IOException | RuntimeException e) {
            mostrarMensaje("La venta se registró, pero no se pudo abrir la boleta: " + e.getMessage(), true);
            return false;
        }
    }


    private void actualizarTotales() {
        Totales totales = calcularTotales();
        lblNeto.setText("S/ " + dinero(totales.neto()));
        lblIgv.setText("S/ " + dinero(totales.igv()));
        lblTotal.setText("S/ " + dinero(totales.total()));
    }

    private Totales calcularTotales() {
        BigDecimal total = lineas.stream().map(LineaCarrito::getSubtotalDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = total.divide(BigDecimal.ONE.add(IGV), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(neto).setScale(2, RoundingMode.HALF_UP);
        return new Totales(neto, igv, total);
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b3261e;" : "-fx-text-fill: #26734d;");
    }

    private static String dinero(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    private static String dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatearCantidad(Double cantidad) {
        if (cantidad == null) return "";
        return cantidad == Math.rint(cantidad) ? String.valueOf(cantidad.intValue()) : String.valueOf(cantidad);
    }

    private static String describirPersonalizacion(Presentacion opcion) {
        String unidad = opcion.getUnidadMedida() == null ? ""
                : " " + opcion.getUnidadMedida().getNombreMedida();
        return opcion.getNombreOpcion() + " (" + formatearCantidad(opcion.getCantidadMedida()) + unidad + ")";
    }

    private record Totales(BigDecimal neto, BigDecimal igv, BigDecimal total) { }

    public static class LineaCarrito {
        private final String producto;
        private final String detalle;
        private final int cantidad;
        private final BigDecimal precioDecimal;

        public LineaCarrito(String producto, String detalle, int cantidad, BigDecimal precio) {
            this.producto = producto;
            this.detalle = detalle;
            this.cantidad = cantidad;
            this.precioDecimal = precio.setScale(2, RoundingMode.HALF_UP);
        }
        public String getProducto() { return producto; }
        public String getDetalle() { return detalle; }
        public int getCantidad() { return cantidad; }
        public String getPrecio() { return "S/ " + precioDecimal; }
        public String getSubtotal() { return "S/ " + getSubtotalDecimal().setScale(2, RoundingMode.HALF_UP); }
        public BigDecimal getSubtotalDecimal() { return precioDecimal.multiply(BigDecimal.valueOf(cantidad)); }
        public boolean esMismaLinea(String otroProducto, String otroDetalle, BigDecimal otroPrecio) {
            return producto.equals(otroProducto)
                    && detalle.equals(otroDetalle)
                    && precioDecimal.compareTo(otroPrecio.setScale(2, RoundingMode.HALF_UP)) == 0;
        }
        public LineaCarrito conCantidad(int nuevaCantidad) {
            return new LineaCarrito(producto, detalle, nuevaCantidad, precioDecimal);
        }
    }

    private static class OpcionVenta {
        private final String nombre;
        private final double incremento;
        private OpcionVenta(String nombre, double incremento) { this.nombre = nombre; this.incremento = incremento; }
        @Override public String toString() {
            if (nombre.equals("Única")) return nombre;
            return nombre + " + S/ " + String.format(java.util.Locale.US, "%.2f", incremento);
        }
    }
}
