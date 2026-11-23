package pe.edu.upeu.sysvstarbucks.utils;

import javafx.scene.Scene;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;

/** Preferencias de idioma y tema, con archivos externos y recursos empaquetados como respaldo. */
public final class PreferenciasUI {
    private static final Preferences PREFERENCIAS = Preferences.userNodeForPackage(PreferenciasUI.class);
    private static final Set<String> IDIOMAS = Set.of("es", "en", "fr");
    private static final Set<String> TEMAS = Set.of("verde", "oscuro", "azul", "rosado", "profesional", "epico");

    private PreferenciasUI() { }

    public static String idiomaActual() {
        String guardado = PREFERENCIAS.get("IDIOMAX", null);
        if (guardado != null && IDIOMAS.contains(guardado)) return guardado;
        String inicial = leerIdiomaInicial();
        PREFERENCIAS.put("IDIOMAX", inicial);
        return inicial;
    }

    public static void guardarIdioma(String idioma) {
        PREFERENCIAS.put("IDIOMAX", idioma != null && IDIOMAS.contains(idioma) ? idioma : "es");
    }

    public static String temaActual() {
        String guardado = PREFERENCIAS.get("TEMA", "verde");
        return guardado != null && TEMAS.contains(guardado) ? guardado : "verde";
    }

    public static void guardarTema(String tema) {
        PREFERENCIAS.put("TEMA", tema != null && TEMAS.contains(tema) ? tema : "verde");
    }

    public static Properties cargarIdioma() {
        String idioma = idiomaActual();
        Properties propiedades = new Properties();
        String archivo = "idiomas-" + idioma + ".properties";
        Path externo = Path.of("language", archivo);
        if (Files.isRegularFile(externo)) {
            try (Reader lector = Files.newBufferedReader(externo, StandardCharsets.UTF_8)) {
                propiedades.load(lector);
                return propiedades;
            } catch (IOException ignored) {
                // Si el archivo externo no se puede leer, se intenta con la copia empaquetada.
            }
        }

        try (InputStream recurso = PreferenciasUI.class.getResourceAsStream("/language/" + archivo)) {
            if (recurso != null) {
                try (Reader lector = new InputStreamReader(recurso, StandardCharsets.UTF_8)) {
                    propiedades.load(lector);
                }
            }
        } catch (IOException ignored) {
            // Las llamadas a texto(...) mantienen una traducción de respaldo.
        }
        return propiedades;
    }

    public static void aplicarTema(Scene escena) {
        if (escena == null) return;
        String archivo = switch (temaActual()) {
            case "oscuro" -> "estilo-oscuro.css";
            case "azul" -> "estilo-azul.css";
            case "rosado" -> "estilo-rosado.css";
            case "profesional" -> "estilo-profesional.css";
            case "epico" -> "estilo_epico.css";
            default -> "estilo-verde.css";
        };
        var recurso = PreferenciasUI.class.getResource("/css/" + archivo);
        if (recurso != null) escena.getStylesheets().setAll(recurso.toExternalForm());
    }

    private static String leerIdiomaInicial() {
        Properties configuracion = new Properties();
        Path externo = Path.of("language", "SysCenterLife.properties");
        if (Files.isRegularFile(externo)) {
            try (Reader lector = Files.newBufferedReader(externo, StandardCharsets.UTF_8)) {
                configuracion.load(lector);
                return idiomaValido(configuracion.getProperty("syscenterlife.idioma"));
            } catch (IOException ignored) {
                // Se intenta con la configuración empaquetada.
            }
        }
        try (InputStream recurso = PreferenciasUI.class.getResourceAsStream("/language/SysCenterLife.properties")) {
            if (recurso != null) {
                try (Reader lector = new InputStreamReader(recurso, StandardCharsets.UTF_8)) {
                    configuracion.load(lector);
                    return idiomaValido(configuracion.getProperty("syscenterlife.idioma"));
                }
            }
        } catch (IOException ignored) {
            // Español es el idioma inicial si falta el archivo de configuración.
        }
        return "es";
    }

    private static String idiomaValido(String idioma) {
        return idioma != null && IDIOMAS.contains(idioma) ? idioma : "es";
    }
}
