package pe.edu.upeu.sysvstarbucks.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Venta {
    private Long idVenta;
    private Double neto;
    private Double igv;
    private Double total;
    private Usuario usuario;
    private Cliente cliente;
    private List<DetalleVenta> detalles;
}