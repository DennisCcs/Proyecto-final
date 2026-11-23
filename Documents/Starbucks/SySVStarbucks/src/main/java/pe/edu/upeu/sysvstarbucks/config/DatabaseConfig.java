package pe.edu.upeu.sysvstarbucks.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String PROPERTIES_RESOURCE = "/application.properties";
    private static final String DEFAULT_URL = "jdbc:h2:file:./data/starbucks;FILE_LOCK=FILE";
    private static volatile Properties applicationProperties;

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        Properties properties = properties();
        return DriverManager.getConnection(
                properties.getProperty("db.url", DEFAULT_URL),
                properties.getProperty("db.username", "sa"),
                properties.getProperty("db.password", ""));
    }

    public static void initializeDatabase() {
        try {
            Properties properties = properties();
            properties.setProperty("db.url", prepararRutaBase(properties.getProperty("db.url", DEFAULT_URL)));
            Class.forName("org.h2.Driver");

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                if (Boolean.parseBoolean(properties.getProperty("db.ddl.auto", "true"))) {
                    ejecutarScript(statement, properties.getProperty("db.ddl.script", "schema_sv.sql"));
                }
                if (Boolean.parseBoolean(properties.getProperty("db.seed.auto", "true"))) {
                    ejecutarScriptsIniciales(statement, properties);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo iniciar la base de datos local H2.", e);
        }
    }

    private static void ejecutarScriptsIniciales(Statement statement, Properties properties)
            throws IOException, SQLException {
        String scripts = properties.getProperty("db.seed.scripts",
                properties.getProperty("db.seed.script", ""));
        for (String script : scripts.split(",")) {
            String nombre = script.trim();
            if (!nombre.isEmpty()) ejecutarScript(statement, nombre);
        }
    }

    private static void ejecutarScript(Statement statement, String nombre)
            throws IOException, SQLException {
        try (InputStream input = DatabaseConfig.class.getResourceAsStream("/" + nombre)) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el script SQL: " + nombre);
            }
            String contenido = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String sql = contenido.lines()
                    .filter(linea -> !linea.stripLeading().startsWith("--"))
                    .reduce("", (acumulado, linea) -> acumulado + linea + "\n");
            for (String sentencia : sql.split(";")) {
                if (!sentencia.isBlank()) statement.execute(sentencia);
            }
        }
    }

    private static Properties properties() {
        Properties loaded = applicationProperties;
        if (loaded != null) return loaded;
        synchronized (DatabaseConfig.class) {
            if (applicationProperties == null) {
                Properties properties = new Properties();
                try (InputStream input = DatabaseConfig.class.getResourceAsStream(PROPERTIES_RESOURCE)) {
                    if (input == null) {
                        throw new IllegalStateException("No se encontró application.properties.");
                    }
                    properties.load(input);
                } catch (IOException e) {
                    throw new IllegalStateException("No se pudo leer application.properties.", e);
                }
                applicationProperties = properties;
            }
            return applicationProperties;
        }
    }

    private static String prepararRutaBase(String url) throws IOException {
        String prefijo = "jdbc:h2:file:";
        if (!url.startsWith(prefijo)) return url;

        String rutaYParametros = url.substring(prefijo.length());
        String[] partes = rutaYParametros.split(";", 2);
        Path archivo = Path.of(partes[0]);
        Path carpeta = archivo.toAbsolutePath().getParent();
        try {
            if (carpeta != null) Files.createDirectories(carpeta);
            return url;
        } catch (IOException | SecurityException errorRutaActual) {
            // Si Windows inició la app desde una carpeta protegida, usa una ubicación del usuario.
            String localAppData = System.getenv("LOCALAPPDATA");
            Path carpetaUsuario = (localAppData == null || localAppData.isBlank())
                    ? Path.of(System.getProperty("user.home"), ".sysvstarbucks", "data")
                    : Path.of(localAppData, "SySVStarbucks", "data");
            Files.createDirectories(carpetaUsuario);

            String parametros = partes.length > 1 ? ";" + partes[1] : "";
            return prefijo + carpetaUsuario.resolve(archivo.getFileName()) + parametros;
        }
    }
}


