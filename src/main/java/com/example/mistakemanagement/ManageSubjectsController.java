package com.example.mistakemanagement;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ManageSubjectsController {
    @FXML
    private TextField subjectNameField;
    
    @FXML
    private TextField subjectDescField;
    
    @FXML
    private Button addSubjectBtn;
    
    @FXML
    private ListView<String> subjectsListView;
    
    @FXML
    private Button deleteSubjectBtn;
    
    @FXML
    private Button refreshBtn;
    
    @FXML
    private Button closeBtn;
    
    @FXML
    private Label statusLabel;
    
    private List<Subject> subjects = new ArrayList<>();

    @FXML
    public void initialize() {
        loadSubjects();
        
        // Add listener for selection
        subjectsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            deleteSubjectBtn.setDisable(newVal == null);
        });
        
        deleteSubjectBtn.setDisable(true);
    }

    private void loadSubjects() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String query = "SELECT id, name, description FROM subjects ORDER BY name";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                subjects.clear();
                List<String> displayItems = new ArrayList<>();
                
                while (rs.next()) {
                    Subject subject = new Subject();
                    subject.id = rs.getInt("id");
                    subject.name = rs.getString("name");
                    subject.description = rs.getString("description");
                    
                    subjects.add(subject);
                    
                    String displayText = String.format("%s - %s", 
                        subject.name, 
                        subject.description != null ? subject.description : "No description");
                    displayItems.add(displayText);
                }
                
                subjectsListView.getItems().setAll(displayItems);
            }
        } catch (SQLException e) {
            statusLabel.setText("Error loading subjects: " + e.getMessage());
        }
    }

    @FXML
    protected void onAddSubjectClick() {
        String name = subjectNameField.getText().trim();
        String description = subjectDescField.getText().trim();
        
        if (name.isEmpty()) {
            statusLabel.setText("Please enter a subject name");
            return;
        }
        
        try (Connection conn = MySQLConnection.getConnection()) {
            // Check if subject already exists
            String checkQuery = "SELECT COUNT(*) FROM subjects WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                stmt.setString(1, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        statusLabel.setText("Subject already exists");
                        return;
                    }
                }
            }
            
            // Insert new subject
            String insertQuery = "INSERT INTO subjects (name, description) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setString(1, name);
                stmt.setString(2, description.isEmpty() ? null : description);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    statusLabel.setText("Subject added successfully!");
                    statusLabel.setStyle("-fx-text-fill: #27ae60");
                    
                    // Clear fields
                    subjectNameField.clear();
                    subjectDescField.clear();
                    
                    // Refresh list
                    loadSubjects();
                } else {
                    statusLabel.setText("Failed to add subject");
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Error adding subject: " + e.getMessage());
        }
    }

    @FXML
    protected void onDeleteSubjectClick() {
        int selectedIndex = subjectsListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= subjects.size()) {
            statusLabel.setText("Please select a subject to delete");
            return;
        }
        
        Subject selectedSubject = subjects.get(selectedIndex);
        
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Subject");
        alert.setContentText("Are you sure you want to delete '" + selectedSubject.name + "'?\n" +
                           "This will also delete all associated mistakes and categories.");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = MySQLConnection.getConnection()) {
                String query = "DELETE FROM subjects WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, selectedSubject.id);
                    
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        statusLabel.setText("Subject deleted successfully!");
                        statusLabel.setStyle("-fx-text-fill: #27ae60");
                        loadSubjects();
                    } else {
                        statusLabel.setText("Failed to delete subject");
                    }
                }
            } catch (SQLException e) {
                statusLabel.setText("Error deleting subject: " + e.getMessage());
            }
        }
    }

    @FXML
    protected void onRefreshClick() {
        loadSubjects();
        statusLabel.setText("Subjects refreshed");
    }

    @FXML
    protected void onCloseClick() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    // Inner class to hold subject data
    private static class Subject {
        int id;
        String name;
        String description;
    }
}
