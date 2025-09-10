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

public class ViewMistakesController {
    @FXML
    private ComboBox<String> subjectFilterCombo;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button refreshBtn;
    
    @FXML
    private ListView<String> mistakesListView;
    
    @FXML
    private Button viewDetailsBtn;
    
    @FXML
    private Button closeBtn;
    
    @FXML
    private Button markReviewedBtn;
    
    @FXML
    private Label statusLabel;
    
    private List<Mistake> mistakes = new ArrayList<>();
    private Mistake selectedMistake = null;

    @FXML
    public void initialize() {
        loadSubjects();
        loadMistakes();
        
        // Add listeners
        subjectFilterCombo.setOnAction(e -> loadMistakes());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterMistakes());
        mistakesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int index = mistakesListView.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < mistakes.size()) {
                    selectedMistake = mistakes.get(index);
                }
            }
        });
    }

    private void loadSubjects() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String query = "SELECT name FROM subjects ORDER BY name";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<String> subjects = new ArrayList<>();
                subjects.add("All Subjects");
                while (rs.next()) {
                    subjects.add(rs.getString("name"));
                }
                subjectFilterCombo.getItems().setAll(subjects);
                subjectFilterCombo.setValue("All Subjects");
            }
        } catch (SQLException e) {
            statusLabel.setText("Error loading subjects: " + e.getMessage());
        }
    }

    private void loadMistakes() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String query = "SELECT m.id, m.title, m.description, m.correct_answer, m.your_answer, " +
                          "m.explanation, m.difficulty_level, m.source, m.is_reviewed, m.review_count, " +
                          "s.name as subject_name, c.name as category_name " +
                          "FROM mistakes m " +
                          "JOIN subjects s ON m.subject_id = s.id " +
                          "LEFT JOIN categories c ON m.category_id = c.id " +
                          "ORDER BY m.created_at DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                mistakes.clear();
                List<String> displayItems = new ArrayList<>();
                
                while (rs.next()) {
                    Mistake mistake = new Mistake();
                    mistake.id = rs.getInt("id");
                    mistake.title = rs.getString("title");
                    mistake.description = rs.getString("description");
                    mistake.correctAnswer = rs.getString("correct_answer");
                    mistake.yourAnswer = rs.getString("your_answer");
                    mistake.explanation = rs.getString("explanation");
                    mistake.difficultyLevel = rs.getString("difficulty_level");
                    mistake.source = rs.getString("source");
                    mistake.isReviewed = rs.getBoolean("is_reviewed");
                    mistake.reviewCount = rs.getInt("review_count");
                    mistake.subjectName = rs.getString("subject_name");
                    mistake.categoryName = rs.getString("category_name");
                    
                    mistakes.add(mistake);
                    
                    // Create display string
                    String reviewed = mistake.isReviewed ? "✓" : "○";
                    String displayText = String.format("[%s] %s - %s (%s) %s",
                        reviewed, mistake.title, mistake.subjectName, 
                        mistake.difficultyLevel, mistake.categoryName != null ? "- " + mistake.categoryName : "");
                    
                    displayItems.add(displayText);
                }
                
                mistakesListView.getItems().setAll(displayItems);
            }
        } catch (SQLException e) {
            statusLabel.setText("Error loading mistakes: " + e.getMessage());
        }
    }

    private void filterMistakes() {
        String searchText = searchField.getText().toLowerCase();
        String selectedSubject = subjectFilterCombo.getValue();
        
        List<String> filteredItems = new ArrayList<>();
        
        for (int i = 0; i < mistakes.size(); i++) {
            Mistake mistake = mistakes.get(i);
            
            // Check subject filter
            boolean subjectMatch = selectedSubject == null || selectedSubject.equals("All Subjects") || 
                                 mistake.subjectName.equals(selectedSubject);
            
            // Check search filter
            boolean searchMatch = searchText.isEmpty() || 
                                 mistake.title.toLowerCase().contains(searchText) ||
                                 (mistake.description != null && mistake.description.toLowerCase().contains(searchText)) ||
                                 mistake.subjectName.toLowerCase().contains(searchText) ||
                                 (mistake.categoryName != null && mistake.categoryName.toLowerCase().contains(searchText));
            
            if (subjectMatch && searchMatch) {
                String reviewed = mistake.isReviewed ? "✓" : "○";
                String displayText = String.format("[%s] %s - %s (%s) %s",
                    reviewed, mistake.title, mistake.subjectName, 
                    mistake.difficultyLevel, mistake.categoryName != null ? "- " + mistake.categoryName : "");
                filteredItems.add(displayText);
            }
        }
        
        mistakesListView.getItems().setAll(filteredItems);
    }

    @FXML
    protected void onRefreshClick() {
        loadMistakes();
        statusLabel.setText("Mistakes refreshed");
    }

    @FXML
    protected void onViewDetailsClick() {
        if (selectedMistake == null) {
            statusLabel.setText("Please select a mistake to view details");
            return;
        }
        
        // Create a simple details dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mistake Details");
        alert.setHeaderText(selectedMistake.title);
        
        StringBuilder content = new StringBuilder();
        content.append("Subject: ").append(selectedMistake.subjectName).append("\n");
        if (selectedMistake.categoryName != null) {
            content.append("Category: ").append(selectedMistake.categoryName).append("\n");
        }
        content.append("Difficulty: ").append(selectedMistake.difficultyLevel).append("\n");
        if (selectedMistake.source != null) {
            content.append("Source: ").append(selectedMistake.source).append("\n");
        }
        content.append("Review Count: ").append(selectedMistake.reviewCount).append("\n\n");
        
        if (selectedMistake.description != null && !selectedMistake.description.isEmpty()) {
            content.append("Description:\n").append(selectedMistake.description).append("\n\n");
        }
        
        if (selectedMistake.yourAnswer != null && !selectedMistake.yourAnswer.isEmpty()) {
            content.append("Your Answer:\n").append(selectedMistake.yourAnswer).append("\n\n");
        }
        
        content.append("Correct Answer:\n").append(selectedMistake.correctAnswer).append("\n\n");
        
        if (selectedMistake.explanation != null && !selectedMistake.explanation.isEmpty()) {
            content.append("Explanation:\n").append(selectedMistake.explanation);
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    @FXML
    protected void onMarkReviewedClick() {
        if (selectedMistake == null) {
            statusLabel.setText("Please select a mistake to mark as reviewed");
            return;
        }
        
        try (Connection conn = MySQLConnection.getConnection()) {
            String query = "UPDATE mistakes SET is_reviewed = true, review_count = review_count + 1, " +
                          "last_reviewed_at = CURRENT_TIMESTAMP WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, selectedMistake.id);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    statusLabel.setText("Mistake marked as reviewed!");
                    statusLabel.setStyle("-fx-text-fill: #27ae60");
                    loadMistakes(); // Refresh the list
                } else {
                    statusLabel.setText("Failed to update mistake");
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Error updating mistake: " + e.getMessage());
        }
    }

    @FXML
    protected void onCloseClick() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    // Inner class to hold mistake data
    private static class Mistake {
        int id;
        String title;
        String description;
        String correctAnswer;
        String yourAnswer;
        String explanation;
        String difficultyLevel;
        String source;
        boolean isReviewed;
        int reviewCount;
        String subjectName;
        String categoryName;
    }
}
