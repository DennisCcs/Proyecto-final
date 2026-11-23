package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Producto {
    private Long idProducto;
    private String nombre;
    private Double precioBase;
    private String descripcion;
    private Menu menu;
}
