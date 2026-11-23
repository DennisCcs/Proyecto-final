package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompCarritoDetalle {
    private Long idCompCarritoDetalle;
    private CompCarrito compCarrito;
    private Insumo insumo;
    private Double cantidad;
    private Double precioUnitario;
    private Double subtotal;
}