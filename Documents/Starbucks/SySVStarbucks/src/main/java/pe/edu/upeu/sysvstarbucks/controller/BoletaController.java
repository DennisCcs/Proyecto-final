package pe.edu.upeu.sysvstarbucks.controller;

import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import pe.edu.upeu.sysvstarbucks.components.comprob.BoletaPdfService;
import pe.edu.upeu.sysvstarbucks.components.comprob.BoletaView;
import pe.edu.upeu.sysvstarbucks.dto.comprobante.BoletaVenta;

import java.io.File;
import java.nio.file.Path;

public class BoletaController {
    @FXML private ScrollPane scrollVisor;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnImprimir;
    private final BoletaPdfService pdfService = new BoletaPdfService();
    private BoletaVenta boleta;
    private VBox vistaActual;

    @FXML private void initialize() {
        btnExportarPdf.setDisable(true); btnImprimir.setDisable(true);
    }

    public void setBoleta(BoletaVenta boleta) {
        this.boleta = boleta;
        vistaActual = new BoletaView(boleta).construir();
        VBox fondo = new VBox(vistaActual);
        fondo.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        fondo.setStyle("-fx-background-color: #e0e4e1; -fx-padding: 18;");
        scrollVisor.setContent(fondo);
        btnExportarPdf.setDisable(false); btnImprimir.setDisable(false);
    }

    @FXML private void onExportarPdf() {
        if (boleta == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar boleta como PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("boleta_" + boleta.codigo().replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf");
        File archivo = chooser.showSaveDialog(btnExportarPdf.getScene().getWindow());
        if (archivo == null) return;
        Path destino = archivo.toPath();
        if (!archivo.getName().toLowerCase().endsWith(".pdf")) destino = Path.of(archivo.getAbsolutePath() + ".pdf");
        try {
            pdfService.exportar(boleta, destino);
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("PDF generado"); alerta.setHeaderText("La boleta se guardó correctamente");
            alerta.setContentText(destino.toAbsolutePath().toString()); alerta.showAndWait();
        } catch (Exception e) { mostrarError("No se pudo generar el PDF", e.getMessage()); }
    }

    @FXML private void onImprimir() {
        if (vistaActual == null) return;
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            mostrarError("No se encontró una impresora instalada.", "Puedes guardar la boleta como PDF e imprimir el archivo.");
            return;
        }
        if (!job.showPrintDialog(btnImprimir.getScene().getWindow())) return;
        vistaActual.applyCss(); vistaActual.autosize(); vistaActual.layout();
        if (job.printPage(vistaActual)) {
            if (!job.endJob()) mostrarError("La impresora no completó el trabajo.", "Revisa la impresora e inténtalo otra vez.");
        } else {
            job.cancelJob();
            mostrarError("No se pudo enviar la boleta a la impresora.", "Puedes guardarla como PDF e imprimir el archivo.");
        }
    }

    private void mostrarError(String encabezado, String detalle) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Boleta"); alerta.setHeaderText(encabezado);
        alerta.setContentText(detalle == null || detalle.isBlank() ? "Ocurrió un error inesperado." : detalle);
        alerta.showAndWait();
    }
}
