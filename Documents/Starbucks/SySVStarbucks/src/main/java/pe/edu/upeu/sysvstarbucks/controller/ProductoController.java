package pe.edu.upeu.sysvstarbucks.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.StringConverter;
import pe.edu.upeu.sysvstarbucks.model.Menu;
import pe.edu.upeu.sysvstarbucks.model.Producto;
import pe.edu.upeu.sysvstarbucks.repository.MenuRepository;
import pe.edu.upeu.sysvstarbucks.repository.ProductoRepository;

import java.util.Optional;

public class ProductoController {
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecioBase;
    @FXML private TextField txtBuscar;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<Menu> cmbMenu;
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Long> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colMenu;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, String> colDescripcion;
    @FXML private Button btnGuardar;
    @FXML private Label lblMensaje;

    private final ProductoRepository productoRepository = new ProductoRepository();
    private final MenuRepository menuRepository = new MenuRepository();
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private FilteredList<Producto> productosFiltrados;
    private Long idEnEdicion;

    @FXML
    private void initialize() {
        configurarTabla();
        configurarComboMenu();
        configurarFiltro();
        tablaProductos.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, seleccionado) -> {
                    if (seleccionado != null) {
                        cargarEnFormulario(seleccionado);
                    }
                });
        recargarProductos();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioBase"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colMenu.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getMenu() == null ? "" : cell.getValue().getMenu().getNombre()));
        productosFiltrados = new FilteredList<>(productos, producto -> true);
        tablaProductos.setItems(productosFiltrados);
    }

    private void configurarComboMenu() {
        cmbMenu.setItems(FXCollections.observableArrayList(menuRepository.findAll()));
        cmbMenu.setConverter(new StringConverter<>() {
            @Override
            public String toString(Menu menu) {
                return menu == null ? "" : menu.getNombre();
            }

            @Override
            public Menu fromString(String texto) {
                return cmbMenu.getItems().stream()
                        .filter(menu -> menu.getNombre().equals(texto))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((observable, anterior, texto) -> {
            String filtro = texto == null ? "" : texto.trim().toLowerCase();
            productosFiltrados.setPredicate(producto -> filtro.isEmpty()
                    || producto.getNombre().toLowerCase().contains(filtro)
                    || (producto.getMenu() != null
                        && producto.getMenu().getNombre().toLowerCase().contains(filtro)));
        });
    }

    @FXML
    private void guardarProducto() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String textoPrecio = txtPrecioBase.getText() == null ? "" : txtPrecioBase.getText().trim();
        Menu menu = cmbMenu.getValue();

        if (nombre.isEmpty()) {
            mostrarMensaje("Escribe el nombre del producto.", true);
            txtNombre.requestFocus();
            return;
        }
        if (menu == null) {
            mostrarMensaje("Selecciona un menú.", true);
            cmbMenu.requestFocus();
            return;
        }

        final double precio;
        try {
            precio = Double.parseDouble(textoPrecio.replace(',', '.'));
            if (!Double.isFinite(precio) || precio <= 0) {
                mostrarMensaje("El precio debe ser mayor que cero.", true);
                txtPrecioBase.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            mostrarMensaje("Escribe un precio válido, por ejemplo 12.50.", true);
            txtPrecioBase.requestFocus();
            return;
        }

        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecioBase(precio);
        producto.setDescripcion(txtDescripcion.getText() == null ? "" : txtDescripcion.getText().trim());
        producto.setMenu(menu);

        try {
            if (idEnEdicion == null) {
                productoRepository.save(producto);
                mostrarMensaje("Producto guardado.", false);
            } else {
                producto.setIdProducto(idEnEdicion);
                productoRepository.update(producto);
                mostrarMensaje("Producto actualizado.", false);
            }
            limpiarFormulario();
            recargarProductos();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    private void eliminarProducto() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un producto de la tabla para eliminarlo.", true);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar producto");
        confirmacion.setHeaderText("¿Eliminar " + seleccionado.getNombre() + "?");
        confirmacion.setContentText("Esta acción eliminará el producto del catálogo.");
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                productoRepository.deleteById(seleccionado.getIdProducto());
                limpiarFormulario();
                recargarProductos();
                mostrarMensaje("Producto eliminado.", false);
            } catch (RuntimeException e) {
                mostrarMensaje(e.getMessage(), true);
            }
        }
    }

    @FXML
    private void limpiarFormulario() {
        idEnEdicion = null;
        txtNombre.clear();
        txtPrecioBase.clear();
        txtDescripcion.clear();
        cmbMenu.getSelectionModel().clearSelection();
        tablaProductos.getSelectionModel().clearSelection();
        btnGuardar.setText("Guardar producto");
        lblMensaje.setText("");
    }

    private void cargarEnFormulario(Producto producto) {
        idEnEdicion = producto.getIdProducto();
        txtNombre.setText(producto.getNombre());
        txtPrecioBase.setText(String.valueOf(producto.getPrecioBase()));
        txtDescripcion.setText(producto.getDescripcion());
        cmbMenu.getItems().stream()
                .filter(menu -> menu.getIdMenu().equals(producto.getMenu().getIdMenu()))
                .findFirst().ifPresent(cmbMenu::setValue);
        btnGuardar.setText("Actualizar producto");
        lblMensaje.setText("Editando producto #" + idEnEdicion);
        lblMensaje.setStyle("-fx-text-fill: #6b4f3a;");
    }

    private void recargarProductos() {
        try {
            productos.setAll(productoRepository.findAll());
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    private void mostrarMensaje(String mensaje, boolean esError) {
        lblMensaje.setText(mensaje == null ? "Ocurrió un error." : mensaje);
        lblMensaje.setStyle(esError
                ? "-fx-text-fill: #b3261e;"
                : "-fx-text-fill: #26734d;");
    }
}