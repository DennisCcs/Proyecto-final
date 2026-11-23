package pe.edu.upeu.sysvstarbucks.utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pe.edu.upeu.sysvstarbucks.dto.PersonaDni;

import java.io.IOException;
import java.util.Optional;

/** Consulta el DNI de forma interactiva usando el mismo flujo de ejemplo del profesor. */
public class ConsultaDniService {
    private static final String URL = "https://eldni.com/pe/buscar-datos-por-dni";

    public Optional<PersonaDni> consultar(String dni) throws IOException {
        Connection.Response getResponse = Jsoup.connect(URL)
                .userAgent("Mozilla/5.0")
                .timeout(12000)
                .method(Connection.Method.GET)
                .execute();

        Document pagina = getResponse.parse();
        String token = pagina.select("input[name=_token]").attr("value");
        if (token.isBlank()) {
            throw new IOException("La página de consulta no entregó el token esperado.");
        }

        Connection.Response postResponse = Jsoup.connect(URL)
                .userAgent("Mozilla/5.0")
                .timeout(12000)
                .cookies(getResponse.cookies())
                .data("_token", token)
                .data("dni", dni)
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();

        Document resultado = Jsoup.parse(postResponse.body());
        Element fila = resultado.select("table tbody tr").first();
        if (fila == null) return Optional.empty();
        Elements celdas = fila.select("td");
        if (celdas.size() < 4) return Optional.empty();

        return Optional.of(new PersonaDni(
                celdas.get(0).text().trim(),
                celdas.get(1).text().trim(),
                celdas.get(2).text().trim(),
                celdas.get(3).text().trim()));
    }
}
