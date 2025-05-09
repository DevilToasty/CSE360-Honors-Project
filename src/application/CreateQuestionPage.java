package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CreateQuestionPage {

    private Scene scene;
    private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;
    private final User currentUser;
    private final Runnable onCloseCallback; // Callback to refresh questions

    public CreateQuestionPage(DatabaseHelper databaseHelper, QuestionManager questionManager, User currentUser, Runnable onCloseCallback) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
        this.currentUser = currentUser;
        this.onCloseCallback = onCloseCallback;
    }

    public void show(CustomTrackedStage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label headerLabel = new Label("Create New Question");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label titleLabel = new Label("Title:");
        TextField titleField = new TextField();
        titleField.setPrefWidth(600);

        Label textLabel = new Label("Question Text:");
        TextArea textArea = new TextArea();
        textArea.setPrefWidth(600);
        textArea.setPrefHeight(200);
        textArea.setWrapText(true);

        Label wordCountLabel = new Label("Word count: 0");
        wordCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        // to count words
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            int wordCount = newValue.trim().isEmpty() ? 0 : newValue.trim().split("\\s+").length;
            wordCountLabel.setText("Word count: " + wordCount);

            if (wordCount < 10 || wordCount > 300) {
                wordCountLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
            } else {
                wordCountLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> {
            primaryStage.goBack(); 
        });

        
        // check all properties
        Button submitButton = new Button("Submit");
        submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        submitButton.setOnAction(e -> {
            String title = titleField.getText().trim();
            String text = textArea.getText().trim();
            int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;
            int titleWordCount = title.isEmpty() ? 0 : title.split("\\s+").length;

            if (title.isEmpty() || text.isEmpty()) {
                errorLabel.setText("Title and text cannot be empty.");
                return;
            }

            // ensure text is correct length
            if (wordCount < 10) {
                errorLabel.setText("Question must be more than 10 words. Current word count: " + wordCount);
                return;
            } else if (wordCount > 300) {
                errorLabel.setText("Question must not exceed 300 words. Current word count: " + wordCount);
                return;
            } else if (titleWordCount > 30) {
                errorLabel.setText("Title must be less than 30 words.");
                return;
            } else if (titleWordCount < 1) {
                errorLabel.setText("Title is empty.");
                return;
            }

            // possible DB error
            try {
                questionManager.createQuestion(currentUser.getUserName(), title, text);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Question created successfully.", ButtonType.OK);
                alert.showAndWait();

                // update questions when going back
                primaryStage.goBack();
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                errorLabel.setText("Error creating question.");
            }
        });

        // elements HBox
        HBox statusBox = new HBox();
        statusBox.setPadding(new Insets(5, 10, 5, 10));
        statusBox.setAlignment(Pos.CENTER_LEFT);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusBox.getChildren().addAll(wordCountLabel, spacer, errorLabel);

        HBox buttonBox = new HBox(10, cancelButton, submitButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(20));
        formBox.setAlignment(Pos.CENTER_LEFT);
        formBox.getChildren().addAll(headerLabel, titleLabel, titleField, textLabel, textArea, statusBox, buttonBox);

        root.setCenter(formBox);
        scene = new Scene(root, 800, 500);
        primaryStage.showScene(scene);
    }
}
