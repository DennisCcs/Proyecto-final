package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.sysvstarbucks.enums.TipoDocumento;

@Getter
@Setter
public class Cliente {
    private String numeroDocumento;
    private TipoDocumento tipoDocumento;
    private String nombres;
    private String direccion;
}
