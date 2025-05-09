package application;

import databasePart1.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewerInboxPage {

    private Scene scene;
    private final QuestionManager questionManager;
    private final DatabaseHelper databaseHelper;
    private final User currentUser;

    public ReviewerInboxPage(DatabaseHelper databaseHelper, QuestionManager questionManager, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
        this.questionManager = questionManager;
    }

    public void show(CustomTrackedStage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f9f9f9;");

        Button backButton = BackButton.createBackButton(primaryStage);
        Label titleLabel = new Label("Reviewer Inbox");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        BorderPane headerPane = new BorderPane();
        headerPane.setLeft(backButton);
        headerPane.setCenter(titleLabel);
        BorderPane.setAlignment(backButton, Pos.CENTER_LEFT);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        headerPane.setPadding(new Insets(10));
        root.setTop(headerPane);

        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(10));
        contentBox.setAlignment(Pos.TOP_CENTER);

        // list of all messages
        Label inboxLabel = new Label("Private Feedback Received:");
        inboxLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        ListView<PrivateFeedback> inboxListView = new ListView<>();
        ObservableList<PrivateFeedback> messages = FXCollections.observableArrayList(
                databaseHelper.getPrivateFeedbackForReviewer(currentUser.getUserName()) // database method to return list of messages
        );
        inboxListView.setItems(messages);
        inboxListView.setPrefHeight(400);

        TextArea messageDetails = new TextArea();
        messageDetails.setEditable(false);
        messageDetails.setWrapText(true);
        messageDetails.setPrefHeight(150);

        inboxListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if(newVal != null) {
                messageDetails.setText("From: " + newVal.getFromUser() + "\nSubject: " + newVal.getFeedback() + "\n\nReceived: " + newVal.getTimestamp());
            }
        });

        Label replyLabel = new Label("Reply to Selected Feedback:");
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Enter your reply here...");
        replyArea.setPrefHeight(150);
        Button sendReplyButton = new Button("Send Reply");
        sendReplyButton.setOnAction(e -> {
            PrivateFeedback selected = inboxListView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Please select a message to reply to.");
                return;
            }
            String replyText = replyArea.getText().trim();
            if (replyText.isEmpty()) {
                showAlert("Please enter your reply.");
                return;
            }
            PrivateMessage reply = new PrivateMessage(currentUser.getUserName(), selected.getFromUser(), "Re: Feedback", replyText);
            boolean success = databaseHelper.insertPrivateMessage(reply);
            if(success) {
                showAlert("Reply sent.");
                replyArea.clear();
            } else {
                showAlert("Failed to send reply.");
            }
        });

        VBox replyBox = new VBox(10, replyLabel, replyArea, sendReplyButton);
        replyBox.setAlignment(Pos.CENTER);

        contentBox.getChildren().addAll(inboxLabel, inboxListView, messageDetails, replyBox);
        root.setCenter(contentBox);

        scene = new Scene(root, 900, 700);
        primaryStage.showScene(scene);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reviewer Inbox");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
