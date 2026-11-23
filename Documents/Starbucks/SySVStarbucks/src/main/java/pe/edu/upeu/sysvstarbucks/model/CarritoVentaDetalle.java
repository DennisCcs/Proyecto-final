package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarritoVentaDetalle {
    private Long idCarritoVentaDetalle;
    private CarritoVenta carritoVenta;
    private Presentacion presentacion;
    private Integer cantidad;
    private Double precioAplicado;
    private String endulzante;
    private String temperatura;
    private String tipoMolienda;
}
