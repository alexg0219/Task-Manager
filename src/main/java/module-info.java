module org.suffers.aag.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.controlsfx.controls;

    opens org.suffers.aag.demo to javafx.fxml;
    exports org.suffers.aag.demo;
    exports org.suffers.aag.demo.entities;
    opens org.suffers.aag.demo.entities to javafx.fxml;
}