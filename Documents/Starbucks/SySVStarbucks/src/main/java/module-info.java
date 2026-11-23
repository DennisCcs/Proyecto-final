module pe.edu.upeu.sysvstarbucks {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires com.h2database;
    requires org.jsoup;
    requires java.desktop;
    requires com.github.librepdf.openpdf;
    requires static lombok;

    opens pe.edu.upeu.sysvstarbucks to javafx.fxml;
    opens pe.edu.upeu.sysvstarbucks.controller to javafx.fxml, javafx.base;
    opens pe.edu.upeu.sysvstarbucks.model to javafx.base;

    exports pe.edu.upeu.sysvstarbucks;
}
