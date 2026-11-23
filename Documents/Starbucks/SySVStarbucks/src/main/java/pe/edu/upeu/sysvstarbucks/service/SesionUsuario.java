package pe.edu.upeu.sysvstarbucks.service;

import pe.edu.upeu.sysvstarbucks.model.Usuario;

/** Mantiene al usuario autenticado durante la ejecución de la aplicación. */
public final class SesionUsuario {
    private static Usuario usuarioActual;

    private SesionUsuario() { }

    public static Usuario obtenerUsuarioActual() {
        return usuarioActual;
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static void cerrar() {
        usuarioActual = null;
    }
}
