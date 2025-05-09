package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Map;

// might not be used

public class ManageDiscussionPage {

    private Scene scene;
    private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;
    private final User currentUser;
    
    private VBox questionsContainer;
    
    private CustomTrackedStage primaryStage;

    public ManageDiscussionPage(DatabaseHelper databaseHelper, QuestionManager questionManager, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
        this.currentUser = currentUser;
    }
    
    public void show(CustomTrackedStage primaryStage) {
        this.primaryStage = primaryStage;
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f9f9f9;");
        
        Button backButton = BackButton.createBackButton(primaryStage);
        Label titleLabel = new Label("Manage Discussion Settings");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Button saveButton = new Button("Save");
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        saveButton.setOnAction(e -> {
            saveSettings();
        });
        
        BorderPane headerPane = new BorderPane();
        headerPane.setLeft(backButton);
        headerPane.setCenter(titleLabel);
        headerPane.setRight(saveButton);
        BorderPane.setAlignment(backButton, Pos.CENTER_LEFT);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setAlignment(saveButton, Pos.CENTER_RIGHT);
        headerPane.setPadding(new Insets(10));
        
        root.setTop(headerPane);
        
        VBox profileSettingsSection = new VBox(10);
        profileSettingsSection.setPadding(new Insets(10));
        Label profileLabel = new Label("Profile Settings");
        profileLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label rolesLabel = new Label("Current Roles: " + currentUser.getRoles());
        
        Button requestReviewerRoleButton = new Button("Request Reviewer Role");
        requestReviewerRoleButton.setOnAction(e -> {

            requestReviewerRole();
        });
        if (currentUser.getRoles().contains("Reviewer")) {
            requestReviewerRoleButton.setVisible(false);
        }
        
        profileSettingsSection.getChildren().addAll(
        		profileLabel, 
        		rolesLabel, 
        		requestReviewerRoleButton);
        
        // Trusted Reviewers Section
        VBox trustedReviewersSection = new VBox(10);
        trustedReviewersSection.setPadding(new Insets(10));
        Label trustedReviewersLabel = new Label("Trusted Reviewers");
        trustedReviewersLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        VBox trustedReviewersContainer = new VBox(5);
        
        Map<String, Double> map = currentUser.getApprovedReviewers();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String reviewerName = entry.getKey();
            Double reviewerScore = entry.getValue();

            HBox reviewerRow = new HBox(10);
            reviewerRow.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label(reviewerName);
            TextField weightField = new TextField(String.valueOf(reviewerScore));
            weightField.setPrefWidth(50);
            Button removeButton = new Button("Remove");
            removeButton.setOnAction(e -> {

                currentUser.removeApprovedReviewer(reviewerName, databaseHelper);
                trustedReviewersContainer.getChildren().remove(reviewerRow);
            });
            reviewerRow.getChildren().addAll(nameLabel, new Label("Weight:"), weightField, removeButton);
            trustedReviewersContainer.getChildren().add(reviewerRow);
        }

        Button addReviewerButton = new Button("Add Reviewer");
        addReviewerButton.setOnAction(e -> {

            addTrustedReviewer();
        });
        trustedReviewersSection.getChildren().addAll(trustedReviewersLabel, trustedReviewersContainer, addReviewerButton);
        
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(10));
        mainContainer.getChildren().addAll(headerPane, profileSettingsSection, trustedReviewersSection);
        
        questionsContainer = new VBox(10);
        questionsContainer.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(questionsContainer);
        scrollPane.setFitToWidth(true);
        
        VBox contentContainer = new VBox(20);
        contentContainer.getChildren().addAll(mainContainer, scrollPane);
        
        root.setCenter(contentContainer);
        
        scene = new Scene(root, 900, 700);
        primaryStage.showScene(scene);
    }
    
    private void saveSettings() {
        System.out.println("Settings saved.");
    }
    
    private void requestReviewerRole() {
        System.out.println("Reviewer role requested.");
    }
    
    private void addTrustedReviewer() {
        System.out.println("Add trusted reviewer dialog opened.");
    }
}
