package application;

import databasePart1.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class StaffMessageViewer {

    private Scene scene;
    private final DatabaseHelper databaseHelper;
    private final User currentUser;

    public StaffMessageViewer(DatabaseHelper databaseHelper, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
    }

    public void show(CustomTrackedStage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f9f9f9;");

        Button backButton = BackButton.createBackButton(primaryStage);
        Label titleLabel = new Label("Private Messaging (All Messages)");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        BorderPane headerPane = new BorderPane();
        headerPane.setLeft(backButton);
        headerPane.setCenter(titleLabel);
        BorderPane.setAlignment(backButton, Pos.CENTER_LEFT);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        headerPane.setPadding(new Insets(10));
        root.setTop(headerPane);

        TabPane tabPane = new TabPane();
        
        Tab inboxTab = new Tab("Inbox");
        inboxTab.setClosable(false);
        inboxTab.setContent(createInboxPane());
        
        Tab composeTab = new Tab("Compose Message");
        composeTab.setClosable(false);
        composeTab.setContent(createComposePane());
        
        tabPane.getTabs().addAll(inboxTab, composeTab);
        root.setCenter(tabPane);

        scene = new Scene(root, 900, 700);
        primaryStage.showScene(scene);
    }
    
    private VBox createInboxPane() {
        VBox inboxPane = new VBox(10);
        inboxPane.setPadding(new Insets(10));
        inboxPane.setAlignment(Pos.TOP_CENTER);
        
        Label inboxLabel = new Label("Inbox Messages");
        inboxLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        ListView<PrivateMessage> inboxListView = new ListView<>();
        ObservableList<PrivateMessage> messages = FXCollections.observableArrayList(
                databaseHelper.getAllPrivateMessages()
        );
        inboxListView.setItems(messages);
        inboxListView.setPrefHeight(400);
        
        TextArea messageDetails = new TextArea();
        messageDetails.setEditable(false);
        messageDetails.setWrapText(true);
        messageDetails.setPrefHeight(150);
        
        // listener to know when message was selected to populate the text field
        inboxListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                messageDetails.setText("From: " + newVal.getSender() +
                        "\nTo: " + newVal.getRecipient() +
                        "\nSubject: " + newVal.getSubject() +
                        "\n\n" + newVal.getMessage() +
                        "\n\nReceived: " + newVal.getTimestamp());
            }
        });
        
        inboxPane.getChildren().addAll(inboxLabel, inboxListView, messageDetails);
        return inboxPane;
    }
    
    private VBox createComposePane() {
        VBox composePane = new VBox(10);
        composePane.setPadding(new Insets(10));
        composePane.setAlignment(Pos.TOP_CENTER);
        
        Label composeLabel = new Label("Compose New Message");
        composeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        TextField recipientField = new TextField();
        recipientField.setPromptText("Recipient username");
        
        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject");
        
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Enter your message here...");
        messageArea.setPrefHeight(200);
        
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> {
            String recipient = recipientField.getText().trim();
            String subject = subjectField.getText().trim();
            String messageText = messageArea.getText().trim();
            if (recipient.isEmpty() || subject.isEmpty() || messageText.isEmpty()) {
                showAlert("Please fill in recipient, subject, and message.");
                return;
            }
            // create new private message object with data
            PrivateMessage msg = new PrivateMessage(currentUser.getUserName(), recipient, subject, messageText);
            boolean success = databaseHelper.insertPrivateMessage(msg);
            if (success) {
                showAlert("Message sent.");
                recipientField.clear();
                subjectField.clear();
                messageArea.clear();
            } else {
                showAlert("Failed to send message.");
            }
        });
        
        VBox composeControls = new VBox(10, composeLabel, recipientField, subjectField, messageArea, sendButton);
        composeControls.setAlignment(Pos.CENTER);
        composePane.getChildren().add(composeControls);
        return composePane;
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Private Messaging");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
