package pe.edu.upeu.sysvstarbucks.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pe.edu.upeu.sysvstarbucks.model.Proveedor;
import pe.edu.upeu.sysvstarbucks.repository.ProveedorRepository;

/** Caché observable de proveedores persistidos en H2. */
public final class ProveedorSessionStore {
    private static final ObservableList<Proveedor> PROVEEDORES = FXCollections.observableArrayList();
    private static final ProveedorRepository REPOSITORY = new ProveedorRepository();

    private ProveedorSessionStore() { }

    public static ObservableList<Proveedor> obtenerTodos() { return PROVEEDORES; }

    public static void recargar() { PROVEEDORES.setAll(REPOSITORY.findAll()); }

    public static Proveedor buscarPorId(Long id) {
        if (id == null) return null;
        return PROVEEDORES.stream().filter(proveedor -> id.equals(proveedor.getIdProveedor()))
                .findFirst().orElse(null);
    }
}
