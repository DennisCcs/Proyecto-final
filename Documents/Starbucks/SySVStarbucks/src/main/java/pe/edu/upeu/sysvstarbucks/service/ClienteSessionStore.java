package pe.edu.upeu.sysvstarbucks.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pe.edu.upeu.sysvstarbucks.model.Cliente;
import pe.edu.upeu.sysvstarbucks.repository.ClienteRepository;

/** Caché observable de clientes persistidos en H2. */
public final class ClienteSessionStore {
    private static final ObservableList<Cliente> CLIENTES = FXCollections.observableArrayList();
    private static final ClienteRepository REPOSITORY = new ClienteRepository();

    private ClienteSessionStore() { }

    public static ObservableList<Cliente> obtenerTodos() { return CLIENTES; }

    public static void recargar() { CLIENTES.setAll(REPOSITORY.findAll()); }

    public static Cliente buscar(String numeroDocumento) {
        if (numeroDocumento == null) return null;
        return CLIENTES.stream()
                .filter(cliente -> numeroDocumento.equals(cliente.getNumeroDocumento()))
                .findFirst().orElse(null);
    }
}
