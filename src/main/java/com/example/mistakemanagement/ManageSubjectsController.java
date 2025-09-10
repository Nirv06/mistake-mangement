package com.example.mistakemanagement;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageSubjectsController {
    // Individual subject management fields
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
    
    // Course selection fields
    @FXML
    private VBox coursesContainer;
    
    @FXML
    private TextField customSubjectField;
    
    @FXML
    private Button addCustomBtn;
    
    @FXML
    private Button selectAllBtn;
    
    @FXML
    private Button deselectAllBtn;
    
    @FXML
    private Button addSelectedBtn;
    
    // Common fields
    @FXML
    private Button closeBtn;
    
    @FXML
    private Label statusLabel;
    
    private List<Subject> subjects = new ArrayList<>();
    private Map<String, CheckBox> courseCheckBoxes = new HashMap<>();
    private List<String> selectedCourses = new ArrayList<>();

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

    // Course selection methods
    private void loadPredefinedCourses() {
        // Define common academic subjects organized by field
        Map<String, List<String>> courseCategories = new HashMap<>();
        
        // STEM Subjects
        List<String> stemSubjects = new ArrayList<>();
        stemSubjects.add("Mathematics");
        stemSubjects.add("Calculus");
        stemSubjects.add("Linear Algebra");
        stemSubjects.add("Statistics");
        stemSubjects.add("Physics");
        stemSubjects.add("Chemistry");
        stemSubjects.add("Biology");
        stemSubjects.add("Computer Science");
        stemSubjects.add("Programming");
        stemSubjects.add("Data Structures");
        stemSubjects.add("Algorithms");
        stemSubjects.add("Database Systems");
        stemSubjects.add("Software Engineering");
        stemSubjects.add("Machine Learning");
        stemSubjects.add("Artificial Intelligence");
        courseCategories.put("STEM Subjects", stemSubjects);
        
        // Engineering
        List<String> engineeringSubjects = new ArrayList<>();
        engineeringSubjects.add("Mechanical Engineering");
        engineeringSubjects.add("Electrical Engineering");
        engineeringSubjects.add("Civil Engineering");
        engineeringSubjects.add("Chemical Engineering");
        engineeringSubjects.add("Computer Engineering");
        engineeringSubjects.add("Materials Science");
        engineeringSubjects.add("Thermodynamics");
        engineeringSubjects.add("Circuit Analysis");
        engineeringSubjects.add("Structural Analysis");
        courseCategories.put("Engineering", engineeringSubjects);
        
        // Business & Economics
        List<String> businessSubjects = new ArrayList<>();
        businessSubjects.add("Economics");
        businessSubjects.add("Microeconomics");
        businessSubjects.add("Macroeconomics");
        businessSubjects.add("Business Administration");
        businessSubjects.add("Accounting");
        businessSubjects.add("Finance");
        businessSubjects.add("Marketing");
        businessSubjects.add("Management");
        businessSubjects.add("Statistics for Business");
        courseCategories.put("Business & Economics", businessSubjects);
        
        // Liberal Arts
        List<String> liberalArtsSubjects = new ArrayList<>();
        liberalArtsSubjects.add("English Literature");
        liberalArtsSubjects.add("Creative Writing");
        liberalArtsSubjects.add("History");
        liberalArtsSubjects.add("Political Science");
        liberalArtsSubjects.add("Psychology");
        liberalArtsSubjects.add("Sociology");
        liberalArtsSubjects.add("Philosophy");
        liberalArtsSubjects.add("Art History");
        liberalArtsSubjects.add("Foreign Languages");
        courseCategories.put("Liberal Arts", liberalArtsSubjects);
        
        // Health Sciences
        List<String> healthSubjects = new ArrayList<>();
        healthSubjects.add("Medicine");
        healthSubjects.add("Nursing");
        healthSubjects.add("Anatomy");
        healthSubjects.add("Physiology");
        healthSubjects.add("Pharmacology");
        healthSubjects.add("Pathology");
        healthSubjects.add("Biochemistry");
        healthSubjects.add("Public Health");
        courseCategories.put("Health Sciences", healthSubjects);
        
        // Create UI for each category
        for (Map.Entry<String, List<String>> entry : courseCategories.entrySet()) {
            String categoryName = entry.getKey();
            List<String> subjects = entry.getValue();
            
            // Add category header
            Label categoryLabel = new Label(categoryName);
            categoryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
            coursesContainer.getChildren().add(categoryLabel);
            
            // Add subjects in this category
            VBox subjectBox = new VBox(5.0);
            subjectBox.setPadding(new javafx.geometry.Insets(0, 20, 10, 0));
            
            for (String subject : subjects) {
                CheckBox checkBox = new CheckBox(subject);
                checkBox.setOnAction(e -> updateSelectedCourses());
                courseCheckBoxes.put(subject, checkBox);
                subjectBox.getChildren().add(checkBox);
            }
            
            coursesContainer.getChildren().add(subjectBox);
        }
    }

    private void updateSelectedCourses() {
        selectedCourses.clear();
        for (Map.Entry<String, CheckBox> entry : courseCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedCourses.add(entry.getKey());
            }
        }
        
        // Update button text
        if (selectedCourses.isEmpty()) {
            addSelectedBtn.setText("Add Selected Subjects");
        } else {
            addSelectedBtn.setText("Add " + selectedCourses.size() + " Selected Subjects");
        }
    }

    @FXML
    protected void onAddCustomClick() {
        String customSubject = customSubjectField.getText().trim();
        if (customSubject.isEmpty()) {
            statusLabel.setText("Please enter a subject name");
            return;
        }
        
        if (courseCheckBoxes.containsKey(customSubject)) {
            statusLabel.setText("Subject already exists in the list");
            return;
        }
        
        // Add custom subject checkbox
        CheckBox customCheckBox = new CheckBox(customSubject);
        customCheckBox.setOnAction(e -> updateSelectedCourses());
        courseCheckBoxes.put(customSubject, customCheckBox);
        
        // Add to the end of the container
        coursesContainer.getChildren().add(customCheckBox);
        
        customSubjectField.clear();
        statusLabel.setText("Custom subject added: " + customSubject);
        statusLabel.setStyle("-fx-text-fill: #27ae60");
    }

    @FXML
    protected void onSelectAllClick() {
        for (CheckBox checkBox : courseCheckBoxes.values()) {
            checkBox.setSelected(true);
        }
        updateSelectedCourses();
        statusLabel.setText("All subjects selected");
        statusLabel.setStyle("-fx-text-fill: #27ae60");
    }

    @FXML
    protected void onDeselectAllClick() {
        for (CheckBox checkBox : courseCheckBoxes.values()) {
            checkBox.setSelected(false);
        }
        updateSelectedCourses();
        statusLabel.setText("All subjects deselected");
        statusLabel.setStyle("-fx-text-fill: #27ae60");
    }

    @FXML
    protected void onAddSelectedClick() {
        if (selectedCourses.isEmpty()) {
            statusLabel.setText("Please select at least one subject");
            return;
        }
        
        try (Connection conn = MySQLConnection.getConnection()) {
            int addedCount = 0;
            int skippedCount = 0;
            
            for (String subjectName : selectedCourses) {
                // Check if subject already exists
                String checkQuery = "SELECT COUNT(*) FROM subjects WHERE name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                    stmt.setString(1, subjectName);
                    try (var rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            skippedCount++;
                            continue; // Subject already exists
                        }
                    }
                }
                
                // Add new subject
                String insertQuery = "INSERT INTO subjects (name, description) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setString(1, subjectName);
                    stmt.setString(2, "Added from course selection");
                    
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        addedCount++;
                    }
                }
            }
            
            if (addedCount > 0) {
                statusLabel.setText("Successfully added " + addedCount + " subjects" + 
                    (skippedCount > 0 ? " (" + skippedCount + " already existed)" : ""));
                statusLabel.setStyle("-fx-text-fill: #27ae60");
                
                // Clear selections and refresh the existing subjects list
                onDeselectAllClick();
                loadSubjects();
            } else {
                statusLabel.setText("All selected subjects already exist in the database");
            }
            
        } catch (SQLException e) {
            statusLabel.setText("Error adding subjects: " + e.getMessage());
        }
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
