package pe.edu.upeu.sysvstarbucks.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pe.edu.upeu.sysvstarbucks.model.Insumo;
import pe.edu.upeu.sysvstarbucks.repository.InsumoRepository;

/** Caché observable de insumos y existencias persistidos en H2. */
public final class InsumosSessionStore {
    private static final ObservableList<Insumo> INSUMOS = FXCollections.observableArrayList();
    private static final InsumoRepository REPOSITORY = new InsumoRepository();

    private InsumosSessionStore() { }

    public static ObservableList<Insumo> obtenerTodos() { return INSUMOS; }

    public static void recargar() { INSUMOS.setAll(REPOSITORY.findAll()); }

    public static Insumo buscarPorId(Long id) {
        if (id == null) return null;
        return INSUMOS.stream().filter(insumo -> id.equals(insumo.getIdInsumo())).findFirst().orElse(null);
    }
}
