package pe.edu.upeu.sysvstarbucks.model;

import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.sysvstarbucks.enums.EstadoUsuario;

@Getter
@Setter
public class Usuario {
    private Long idUsuario;
    private String usuario;
    private String claveHash;
    private EstadoUsuario estado;
    private Perfil perfil;
}
