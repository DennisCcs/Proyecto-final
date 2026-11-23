package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Emisor {
    private Long idEmisor;
    private String ruc;
    private String nombreComercial;
    private String ubigeo;
    private String domicilioFiscal;
    private String departamento;
    private String provincia;
    private String distrito;
}