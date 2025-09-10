package com.example.mistakemanagement;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        // Test MySQL connection
        boolean connected = MySQLConnection.testConnection();
        
        if (connected) {
            welcomeText.setText("MySQL Connected Successfully!");
        } else {
            welcomeText.setText("MySQL Connection Failed!");
        }
    }
}