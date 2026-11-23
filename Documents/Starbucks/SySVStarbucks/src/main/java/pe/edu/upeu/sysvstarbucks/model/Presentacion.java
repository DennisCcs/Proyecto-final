package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Presentacion {
    private Long idPresentacion;
    private Producto producto;
    private String nombreOpcion;
    private Double incrementoPrecio;
    private Double stock;
    private Double cantidadMedida;
    private UnidadMedida unidadMedida;
}