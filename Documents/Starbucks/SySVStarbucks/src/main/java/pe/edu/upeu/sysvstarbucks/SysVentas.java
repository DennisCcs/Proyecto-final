package pe.edu.upeu.sysvstarbucks;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pe.edu.upeu.sysvstarbucks.config.DatabaseConfig;
import pe.edu.upeu.sysvstarbucks.utils.PreferenciasUI;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SysVentas extends Application {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> guardarErrorInicio(error));
        try {
            launch(args);
        } catch (Throwable error) {
            guardarErrorInicio(error);
            if (error instanceof RuntimeException runtimeException) throw runtimeException;
            if (error instanceof Error fatalError) throw fatalError;
            throw new RuntimeException(error);
        }
    }

    private static void guardarErrorInicio(Throwable error) {
        try {
            StringWriter contenido = new StringWriter();
            error.printStackTrace(new PrintWriter(contenido));
            Path archivo = Path.of(System.getProperty("java.io.tmpdir"), "SySVStarbucks-startup-error.log");
            Files.writeString(archivo, contenido.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // El registro del error no debe ocultar el fallo original.
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        DatabaseConfig.initializeDatabase();

        FXMLLoader fxmlLoader = new FXMLLoader(SysVentas.class.getResource("/view/main_login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 620);
        PreferenciasUI.aplicarTema(scene);
        stage.setTitle("Iniciar sesión - Starbucks");
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }
}
