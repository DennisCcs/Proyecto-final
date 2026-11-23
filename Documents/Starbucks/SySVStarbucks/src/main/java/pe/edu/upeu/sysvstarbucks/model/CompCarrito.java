package pe.edu.upeu.sysvstarbucks.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.sysvstarbucks.enums.EstadoCarrito;

@Getter
@Setter
public class CompCarrito {
    private Long idCompCarrito;
    private EstadoCarrito estado;
    private Usuario usuario;
    private Proveedor proveedor;
    private List<CompCarritoDetalle> detalles;
}