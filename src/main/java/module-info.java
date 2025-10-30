module com.clipedge {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires com.google.gson;
    
    exports com.clipedge;
    opens com.clipedge to com.google.gson;
}