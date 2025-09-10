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

public class AddMistakeController {
    @FXML
    private TextField titleField;
    
    @FXML
    private ComboBox<String> subjectCombo;
    
    @FXML
    private ComboBox<String> categoryCombo;
    
    @FXML
    private ComboBox<String> difficultyCombo;
    
    @FXML
    private TextArea descriptionArea;
    
    @FXML
    private TextArea yourAnswerArea;
    
    @FXML
    private TextArea correctAnswerArea;
    
    @FXML
    private TextArea explanationArea;
    
    @FXML
    private TextField sourceField;
    
    @FXML
    private TextField tagsField;
    
    @FXML
    private Button saveBtn;
    
    @FXML
    private Button clearBtn;
    
    @FXML
    private Button closeBtn;
    
    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        // Initialize difficulty levels
        difficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        difficultyCombo.setValue("Medium");
        
        // Load subjects
        loadSubjects();
        
        // Add listener to subject combo to update categories
        subjectCombo.setOnAction(e -> loadCategories());
    }

    private void loadSubjects() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String query = "SELECT name FROM subjects ORDER BY name";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<String> subjects = new ArrayList<>();
                while (rs.next()) {
                    subjects.add(rs.getString("name"));
                }
                subjectCombo.getItems().setAll(subjects);
            }
        } catch (SQLException e) {
            statusLabel.setText("Error loading subjects: " + e.getMessage());
        }
    }

    private void loadCategories() {
        String selectedSubject = subjectCombo.getValue();
        if (selectedSubject == null) return;
        
        categoryCombo.getItems().clear();
        
        try (Connection conn = MySQLConnection.getConnection()) {
            String query = "SELECT c.name FROM categories c " +
                          "JOIN subjects s ON c.subject_id = s.id " +
                          "WHERE s.name = ? ORDER BY c.name";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, selectedSubject);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<String> categories = new ArrayList<>();
                    while (rs.next()) {
                        categories.add(rs.getString("name"));
                    }
                    categoryCombo.getItems().setAll(categories);
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Error loading categories: " + e.getMessage());
        }
    }

    @FXML
    protected void onSaveClick() {
        // Validate required fields
        if (titleField.getText().trim().isEmpty()) {
            statusLabel.setText("Please enter a title");
            return;
        }
        
        if (subjectCombo.getValue() == null) {
            statusLabel.setText("Please select a subject");
            return;
        }
        
        if (correctAnswerArea.getText().trim().isEmpty()) {
            statusLabel.setText("Please enter the correct answer");
            return;
        }

        try (Connection conn = MySQLConnection.getConnection()) {
            // Get subject ID
            String subjectQuery = "SELECT id FROM subjects WHERE name = ?";
            int subjectId;
            try (PreparedStatement stmt = conn.prepareStatement(subjectQuery)) {
                stmt.setString(1, subjectCombo.getValue());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        statusLabel.setText("Subject not found");
                        return;
                    }
                    subjectId = rs.getInt("id");
                }
            }

            // Get category ID (optional)
            Integer categoryId = null;
            if (categoryCombo.getValue() != null) {
                String categoryQuery = "SELECT id FROM categories WHERE name = ? AND subject_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(categoryQuery)) {
                    stmt.setString(1, categoryCombo.getValue());
                    stmt.setInt(2, subjectId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            categoryId = rs.getInt("id");
                        }
                    }
                }
            }

            // Insert mistake
            String insertQuery = "INSERT INTO mistakes (title, description, correct_answer, your_answer, " +
                               "explanation, difficulty_level, subject_id, category_id, source) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setString(1, titleField.getText().trim());
                stmt.setString(2, descriptionArea.getText().trim());
                stmt.setString(3, correctAnswerArea.getText().trim());
                stmt.setString(4, yourAnswerArea.getText().trim());
                stmt.setString(5, explanationArea.getText().trim());
                stmt.setString(6, difficultyCombo.getValue());
                stmt.setInt(7, subjectId);
                if (categoryId != null) {
                    stmt.setInt(8, categoryId);
                } else {
                    stmt.setNull(8, java.sql.Types.INTEGER);
                }
                stmt.setString(9, sourceField.getText().trim());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    statusLabel.setText("Mistake saved successfully!");
                    statusLabel.setStyle("-fx-text-fill: #27ae60");
                    
                    // Handle tags if provided
                    if (!tagsField.getText().trim().isEmpty()) {
                        addTagsToMistake(conn, getLastInsertedMistakeId(conn));
                    }
                    
                    // Clear form after successful save
                    onClearClick();
                } else {
                    statusLabel.setText("Failed to save mistake");
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Error saving mistake: " + e.getMessage());
        }
    }

    private int getLastInsertedMistakeId(Connection conn) throws SQLException {
        String query = "SELECT LAST_INSERT_ID() as id";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        throw new SQLException("Could not get last inserted ID");
    }

    private void addTagsToMistake(Connection conn, int mistakeId) throws SQLException {
        String[] tagNames = tagsField.getText().trim().split(",");
        
        for (String tagName : tagNames) {
            tagName = tagName.trim();
            if (tagName.isEmpty()) continue;
            
            // Get or create tag
            int tagId = getOrCreateTag(conn, tagName);
            
            // Link mistake to tag
            String linkQuery = "INSERT IGNORE INTO mistake_tags (mistake_id, tag_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(linkQuery)) {
                stmt.setInt(1, mistakeId);
                stmt.setInt(2, tagId);
                stmt.executeUpdate();
            }
        }
    }

    private int getOrCreateTag(Connection conn, String tagName) throws SQLException {
        // Try to get existing tag
        String selectQuery = "SELECT id FROM tags WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
            stmt.setString(1, tagName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        
        // Create new tag
        String insertQuery = "INSERT INTO tags (name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
            stmt.setString(1, tagName);
            stmt.executeUpdate();
            return getLastInsertedMistakeId(conn); // Reuse the method for tag ID
        }
    }

    @FXML
    protected void onClearClick() {
        titleField.clear();
        subjectCombo.setValue(null);
        categoryCombo.getItems().clear();
        difficultyCombo.setValue("Medium");
        descriptionArea.clear();
        yourAnswerArea.clear();
        correctAnswerArea.clear();
        explanationArea.clear();
        sourceField.clear();
        tagsField.clear();
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: #e74c3c");
    }

    @FXML
    protected void onCloseClick() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }
}
