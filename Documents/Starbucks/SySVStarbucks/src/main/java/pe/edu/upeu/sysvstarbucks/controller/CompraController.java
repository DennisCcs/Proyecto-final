package pe.edu.upeu.sysvstarbucks.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import pe.edu.upeu.sysvstarbucks.model.Insumo;
import pe.edu.upeu.sysvstarbucks.model.Proveedor;
import pe.edu.upeu.sysvstarbucks.model.Usuario;
import pe.edu.upeu.sysvstarbucks.repository.CompraRepository;
import pe.edu.upeu.sysvstarbucks.service.CompraSessionStore;
import pe.edu.upeu.sysvstarbucks.service.CompraSessionStore.DetalleCompraGuardado;
import pe.edu.upeu.sysvstarbucks.service.InsumosSessionStore;
import pe.edu.upeu.sysvstarbucks.service.ProveedorSessionStore;
import pe.edu.upeu.sysvstarbucks.service.SesionUsuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CompraController {
    private static final BigDecimal IGV = new BigDecimal("0.18");

    @FXML private ComboBox<Proveedor> cmbProveedor;
    @FXML private Label lblDatosProveedor;
    @FXML private ComboBox<EmpaqueCompra> cmbPaquete;
    @FXML private Label lblContenido;
    @FXML private Label lblStockActual;
    @FXML private Label lblMensaje;
    @FXML private Label lblNeto;
    @FXML private Label lblIgv;
    @FXML private Label lblTotal;
    @FXML private TextField txtCantidadPaquetes;
    @FXML private TextField txtPrecioPaquete;
    @FXML private TableView<LineaCompra> tablaCarrito;
    @FXML private TableColumn<LineaCompra, String> colPaquete;
    @FXML private TableColumn<LineaCompra, String> colInsumo;
    @FXML private TableColumn<LineaCompra, String> colContenido;
    @FXML private TableColumn<LineaCompra, String> colStock;
    @FXML private TableColumn<LineaCompra, String> colCantidad;
    @FXML private TableColumn<LineaCompra, String> colPrecio;
    @FXML private TableColumn<LineaCompra, String> colSubtotal;

    private final ObservableList<LineaCompra> lineas = FXCollections.observableArrayList();
    private final ObservableList<EmpaqueCompra> paquetesDisponibles = FXCollections.observableArrayList();
    private final CompraRepository compraRepository = new CompraRepository();

    @FXML
    private void initialize() {
        ProveedorSessionStore.recargar();
        InsumosSessionStore.recargar();
        txtPrecioPaquete.setEditable(false);
        cmbProveedor.setItems(ProveedorSessionStore.obtenerTodos());
        cmbProveedor.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Proveedor proveedor) {
                return proveedor == null ? "" : proveedor.getNombres();
            }
            @Override public Proveedor fromString(String texto) { return null; }
        });

        cmbPaquete.setItems(paquetesDisponibles);
        cmbPaquete.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(EmpaqueCompra empaque) {
                return empaque == null ? "" : empaque.getNombrePaquete() + " — " + empaque.getInsumo().getNombre();
            }
            @Override public EmpaqueCompra fromString(String texto) { return null; }
        });
        cmbProveedor.valueProperty().addListener((obs, anterior, proveedor) -> cargarPaquetesProveedor(proveedor));
        cmbPaquete.valueProperty().addListener((obs, anterior, empaque) -> mostrarDatosPaquete(empaque));
        configurarTabla();
        actualizarTotales();
    }

    private EmpaqueCompra empaque(Insumo insumo) {
        return new EmpaqueCompra(insumo.getIdInsumo(), insumo.getProveedor(), insumo,
                insumo.getNombrePaquete(), insumo.getContenidoPorPaquete(),
                insumo.getUnidadMedida().getNombreMedida(),
                BigDecimal.valueOf(insumo.getPrecioPaquete()).setScale(2, RoundingMode.HALF_UP));
    }

    private void cargarPaquetesProveedor(Proveedor proveedor) {
        mostrarDatosProveedor(proveedor);
        if (proveedor == null) {
            paquetesDisponibles.clear();
            mostrarDatosPaquete(null);
            return;
        }
        paquetesDisponibles.setAll(InsumosSessionStore.obtenerTodos().stream()
                .filter(insumo -> insumo.getProveedor() != null
                        && insumo.getProveedor().getIdProveedor().equals(proveedor.getIdProveedor())
                        && insumo.getNombrePaquete() != null && insumo.getContenidoPorPaquete() != null
                        && insumo.getPrecioPaquete() != null)
                .map(this::empaque).toList());
        if (!paquetesDisponibles.isEmpty()) {
            cmbPaquete.getSelectionModel().selectFirst();
            mostrarMensaje("Selecciona un paquete e indica cuántos comprar.", false);
        } else {
            mostrarMensaje("Este proveedor todavía no tiene paquetes configurados.", false);
        }
    }

    private void mostrarDatosProveedor(Proveedor proveedor) {
        if (proveedor == null) {
            lblDatosProveedor.setText("Tipo y número de documento: selecciona un proveedor.");
            return;
        }
        String tipo = proveedor.getTipoDocumento() == null
                ? "No registrado" : proveedor.getTipoDocumento().name();
        String numero = proveedor.getNumeroDocumento() == null || proveedor.getNumeroDocumento().isBlank()
                ? "No registrado" : proveedor.getNumeroDocumento();
        lblDatosProveedor.setText("Tipo de documento: " + tipo + "    Número: " + numero);
    }

    private void mostrarDatosPaquete(EmpaqueCompra empaque) {
        if (empaque == null) {
            lblContenido.setText("—");
            txtPrecioPaquete.clear();
            lblStockActual.setText("Stock actual: —");
            return;
        }
        lblContenido.setText(empaque.getTextoContenido());
        txtPrecioPaquete.setText(dinero(empaque.getPrecioPaquete()));
        lblStockActual.setText("Stock actual: " + cantidad(empaque.getInsumo().getStockActual())
                + " " + empaque.getUnidadBase());
    }

    private void configurarTabla() {
        colPaquete.setCellValueFactory(cell -> texto(cell.getValue().getEmpaque().getNombrePaquete()));
        colInsumo.setCellValueFactory(cell -> texto(cell.getValue().getEmpaque().getInsumo().getNombre()));
        colContenido.setCellValueFactory(cell -> texto(cell.getValue().getEmpaque().getTextoContenido()));
        colStock.setCellValueFactory(cell -> texto(cantidad(cell.getValue().getStockAntes())
                + " " + cell.getValue().getEmpaque().getUnidadBase()));
        colCantidad.setCellValueFactory(cell -> texto(String.valueOf(cell.getValue().getCantidadPaquetes())));
        colPrecio.setCellValueFactory(cell -> texto(dinero(cell.getValue().getPrecioPaquete())));
        colSubtotal.setCellValueFactory(cell -> texto(dinero(cell.getValue().subtotalDecimal())));
        tablaCarrito.setItems(lineas);
    }

    @FXML
    private void agregarAlCarrito() {
        Proveedor proveedor = cmbProveedor.getValue();
        EmpaqueCompra empaque = cmbPaquete.getValue();
        if (proveedor == null || empaque == null) {
            mostrarMensaje("Selecciona un proveedor y uno de sus paquetes.", true);
            return;
        }
        int cantidadPaquetes;
        try {
            cantidadPaquetes = Integer.parseInt(txtCantidadPaquetes.getText().trim());
            if (cantidadPaquetes < 1) throw new NumberFormatException();
        } catch (RuntimeException e) {
            mostrarMensaje("Indica una cantidad entera de paquetes mayor que cero.", true);
            return;
        }
        BigDecimal precioAplicado = empaque.getPrecioPaquete();
        if (!lineas.isEmpty() && !lineas.get(0).getEmpaque().getProveedor().getIdProveedor()
                .equals(proveedor.getIdProveedor())) {
            mostrarMensaje("Un carrito de compra corresponde a un solo proveedor. Vacíalo para cambiarlo.", true);
            return;
        }

        LineaCompra existente = lineas.stream().filter(linea -> linea.mismaLinea(empaque, precioAplicado))
                .findFirst().orElse(null);
        if (existente == null) {
            lineas.add(new LineaCompra(empaque, empaque.getInsumo().getStockActual(), cantidadPaquetes, precioAplicado));
            mostrarMensaje("Paquete agregado al carrito.", false);
        } else {
            int indice = lineas.indexOf(existente);
            lineas.set(indice, existente.conCantidad(existente.getCantidadPaquetes() + cantidadPaquetes));
            mostrarMensaje("Se sumó la cantidad en el paquete que ya estaba en el carrito.", false);
        }
        txtCantidadPaquetes.clear();
        cmbProveedor.setDisable(true);
        actualizarTotales();
    }

    @FXML
    private void quitarDelCarrito() {
        LineaCompra seleccionada = tablaCarrito.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Selecciona una línea para quitarla del carrito.", true);
            return;
        }
        lineas.remove(seleccionada);
        actualizarTotales();
        if (lineas.isEmpty()) cmbProveedor.setDisable(false);
        mostrarMensaje("Paquete quitado del carrito.", false);
    }

    @FXML
    private void vaciarCarrito() {
        lineas.clear();
        cmbProveedor.setDisable(false);
        actualizarTotales();
        mostrarMensaje("Carrito de compra vacío.", false);
    }

    @FXML
    private void registrarCompra() {
        if (lineas.isEmpty()) {
            mostrarMensaje("Agrega al menos un paquete antes de registrar la compra.", true);
            return;
        }
        BigDecimal total = lineas.stream().map(LineaCompra::subtotalDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = total.divide(BigDecimal.ONE.add(IGV), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(neto);
        List<DetalleCompraGuardado> detalles = lineas.stream().map(linea -> {
            EmpaqueCompra empaque = linea.getEmpaque();
            Insumo insumo = empaque.getInsumo();
            return new DetalleCompraGuardado(insumo.getIdInsumo(), empaque.getNombrePaquete(), insumo.getNombre(),
                    empaque.getTextoContenido(), empaque.getContenidoTotal(), empaque.getUnidadBase(),
                    linea.getCantidadPaquetes(), linea.getPrecioPaquete(), linea.subtotalDecimal());
        }).toList();
        Proveedor proveedor = lineas.get(0).getEmpaque().getProveedor();
        Usuario usuario = SesionUsuario.obtenerUsuarioActual();
        if (usuario == null || usuario.getIdUsuario() == null || proveedor.getIdProveedor() == null) {
            mostrarMensaje("No se encontró la sesión o el proveedor para registrar la compra.", true);
            return;
        }
        String nombreProveedor = proveedor.getNombres();
        CompraSessionStore.CompraGuardada compraRegistrada;
        try {
            compraRegistrada = compraRepository.registrarCompra(usuario.getIdUsuario(), proveedor.getIdProveedor(),
                    nombreProveedor, detalles, neto, igv, total);
            CompraSessionStore.agregar(compraRegistrada);
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
            return;
        }

        for (LineaCompra linea : lineas) {
            Insumo insumo = linea.getEmpaque().getInsumo();
            double stockActualizado = insumo.getStockActual()
                    + linea.getEmpaque().getContenidoTotal() * linea.getCantidadPaquetes();
            insumo.setStockActual(stockActualizado);
        }
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Compra registrada");
        alerta.setHeaderText("Compra " + compraRegistrada.codigo() + " registrada");
        alerta.setContentText("Proveedor: " + nombreProveedor + "\nTotal: S/ " + dinero(total)
                + "\nEl stock se actualizó según el contenido de los paquetes.");
        alerta.showAndWait();
        lineas.clear();
        cmbProveedor.setDisable(false);
        actualizarTotales();
        mostrarDatosPaquete(cmbPaquete.getValue());
        mostrarMensaje("Compra registrada y stock actualizado.", false);
    }

    private void actualizarTotales() {
        BigDecimal total = lineas.stream().map(LineaCompra::subtotalDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = total.divide(BigDecimal.ONE.add(IGV), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(neto);
        lblNeto.setText("S/ " + dinero(neto));
        lblIgv.setText("S/ " + dinero(igv));
        lblTotal.setText("S/ " + dinero(total));
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b3261e;" : "-fx-text-fill: #26734d;");
    }

    private static String cantidad(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor)
                : String.format(java.util.Locale.US, "%.2f", valor);
    }

    private static String dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }

    public static class EmpaqueCompra {
        private final Long id;
        private final Proveedor proveedor;
        private final Insumo insumo;
        private final String nombrePaquete;
        private final double contenidoTotal;
        private final String unidadBase;
        private final BigDecimal precioPaquete;

        public EmpaqueCompra(Long id, Proveedor proveedor, Insumo insumo, String nombrePaquete,
                             double contenidoTotal, String unidadBase, BigDecimal precioPaquete) {
            this.id = id;
            this.proveedor = proveedor;
            this.insumo = insumo;
            this.nombrePaquete = nombrePaquete;
            this.contenidoTotal = contenidoTotal;
            this.unidadBase = unidadBase;
            this.precioPaquete = precioPaquete;
        }
        public Long getId() { return id; }
        public Proveedor getProveedor() { return proveedor; }
        public Insumo getInsumo() { return insumo; }
        public String getNombrePaquete() { return nombrePaquete; }
        public double getContenidoTotal() { return contenidoTotal; }
        public String getUnidadBase() { return unidadBase; }
        public BigDecimal getPrecioPaquete() { return precioPaquete; }
        public String getTextoContenido() { return cantidad(contenidoTotal) + " " + unidadBase; }
    }

    public static class LineaCompra {
        private final EmpaqueCompra empaque;
        private final double stockAntes;
        private final int cantidadPaquetes;
        private final BigDecimal precioPaquete;

        public LineaCompra(EmpaqueCompra empaque, double stockAntes, int cantidadPaquetes, BigDecimal precioPaquete) {
            this.empaque = empaque;
            this.stockAntes = stockAntes;
            this.cantidadPaquetes = cantidadPaquetes;
            this.precioPaquete = precioPaquete;
        }
        public EmpaqueCompra getEmpaque() { return empaque; }
        public double getStockAntes() { return stockAntes; }
        public int getCantidadPaquetes() { return cantidadPaquetes; }
        public BigDecimal getPrecioPaquete() { return precioPaquete; }
        public BigDecimal subtotalDecimal() {
            return precioPaquete.multiply(BigDecimal.valueOf(cantidadPaquetes))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        public boolean mismaLinea(EmpaqueCompra otroEmpaque, BigDecimal otroPrecio) {
            return empaque.getId().equals(otroEmpaque.getId()) && precioPaquete.compareTo(otroPrecio) == 0;
        }
        public LineaCompra conCantidad(int nuevaCantidad) {
            return new LineaCompra(empaque, stockAntes, nuevaCantidad, precioPaquete);
        }
    }
}


