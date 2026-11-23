package pe.edu.upeu.sysvstarbucks.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.sysvstarbucks.enums.EstadoCarrito;

@Getter
@Setter
public class CarritoVenta {
    private Long idCarritoVenta;
    private EstadoCarrito estado;
    private Usuario usuario;
    private Cliente cliente;
    private List<CarritoVentaDetalle> detalles;
}