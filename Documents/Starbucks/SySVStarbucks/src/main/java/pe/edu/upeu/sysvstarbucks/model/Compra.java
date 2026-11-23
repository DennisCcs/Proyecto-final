package pe.edu.upeu.sysvstarbucks.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Compra {
    private Long idCompra;
    private Double neto;
    private Double igv;
    private Double total;
    private Usuario usuario;
    private Proveedor proveedor;
    private List<CompraDetalle> detalles;
}