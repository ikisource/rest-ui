module fr.ikisource.restui {

    requires javafx.fxml;
    requires javafx.web;
    requires java.logging;
    requires jdk.crypto.ec; // security (SSL handshake failed)

    // automatic modules
    requires org.jdom2;
    requires com.fasterxml.jackson.databind;
//    requires jersey.client;
    requires java.net.http;
//    requires jersey.core;

    // opens package 'controller' to the module 'javafx.fxml'
    opens fr.ikisource.restui.controller to javafx.fxml;

    // opens package 'model' to the module javafx.base
    opens fr.ikisource.restui.model to javafx.base;

    // opens fxml location (otherwise javafx.fxml.LoadException: Cannot resolve path: /fxml/bottom.fxml)
//    opens fxml;

    // exported package
    exports fr.ikisource.restui.gui;
}