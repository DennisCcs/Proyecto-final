package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Insumo {
    private Long idInsumo;
    private String nombre;
    private String descripcion;
    private Double stockActual;
    private Double stockMinimo;
    private UnidadMedida unidadMedida;
    private Proveedor proveedor;
    private String nombrePaquete;
    private Double contenidoPorPaquete;
    private Double precioPaquete;
}
