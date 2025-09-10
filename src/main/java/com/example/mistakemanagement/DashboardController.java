package com.example.mistakemanagement;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardController {
    @FXML
    private Label welcomeText;
    
    @FXML
    private Button addMistakeBtn;
    
    @FXML
    private Button viewMistakesBtn;
    
    @FXML
    private Button manageSubjectsBtn;
    
    @FXML
    private Button testDbBtn;
    
    @FXML
    private Label statsLabel;

    @FXML
    protected void onAddMistakeClick() {
        try {
            // Check if resource exists
            java.net.URL resource = DashboardController.class.getResource("add-mistake-view.fxml");
            if (resource == null) {
                welcomeText.setText("FXML file not found: add-mistake-view.fxml");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Scene scene = new Scene(loader.load(), 900, 800);
            Stage stage = new Stage();
            stage.setTitle("Add New Mistake");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            welcomeText.setText("Error loading add mistake form: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        } catch (Exception e) {
            welcomeText.setText("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void onViewMistakesClick() {
        try {
            FXMLLoader loader = new FXMLLoader(DashboardController.class.getResource("view-mistakes-view.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            Stage stage = new Stage();
            stage.setTitle("View All Mistakes");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            welcomeText.setText("Error loading view mistakes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void onManageSubjectsClick() {
        try {
            // Check if resource exists
            java.net.URL resource = DashboardController.class.getResource("manage-subjects-view.fxml");
            if (resource == null) {
                welcomeText.setText("FXML file not found: manage-subjects-view.fxml");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Scene scene = new Scene(loader.load(), 750, 650);
            Stage stage = new Stage();
            stage.setTitle("Subject Management");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            welcomeText.setText("Error loading subject management: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            welcomeText.setText("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void onTestDbClick() {
        boolean connected = MySQLConnection.testConnection();
        if (connected) {
            welcomeText.setText("Database connection successful!");
            loadStats();
        } else {
            welcomeText.setText("Database connection failed!");
        }
    }
    
    private void loadStats() {
        try (Connection conn = MySQLConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Get total mistakes count
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM mistakes");
            int totalMistakes = rs.next() ? rs.getInt("total") : 0;
            
            // Get subjects count
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM subjects");
            int totalSubjects = rs.next() ? rs.getInt("total") : 0;
            
            statsLabel.setText(String.format("Stats: %d Mistakes | %d Subjects", totalMistakes, totalSubjects));
            
        } catch (Exception e) {
            statsLabel.setText("Stats: Error loading data");
        }
    }
}
