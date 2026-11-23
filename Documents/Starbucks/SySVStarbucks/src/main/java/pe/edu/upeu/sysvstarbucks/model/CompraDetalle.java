package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompraDetalle {
    private Long idCompraDetalle;
    private Compra compra;
    private Insumo insumo;
    private Double cantidad;
    private Double precioUnitario;
    private Double subtotal;
}