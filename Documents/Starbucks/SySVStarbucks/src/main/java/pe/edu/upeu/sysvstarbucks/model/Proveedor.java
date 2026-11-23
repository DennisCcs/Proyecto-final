package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.sysvstarbucks.enums.TipoDocumento;

@Getter
@Setter
public class Proveedor {
    private Long idProveedor;
    private String numeroDocumento;
    private TipoDocumento tipoDocumento;
    private String nombres;
    private String celular;
    private String correo;
    private String direccion;
}
