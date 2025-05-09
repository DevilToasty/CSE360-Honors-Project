package application;

import databasePart1.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class StudentReviewerInterface {

    private Scene scene;
    private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;
    private final User currentUser;

    public StudentReviewerInterface(DatabaseHelper databaseHelper, QuestionManager questionManager, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
        this.currentUser = currentUser;
    }

    public void show(CustomTrackedStage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f9f9f9;");

        Button backButton = BackButton.createBackButton(primaryStage);
        Label headerLabel = new Label("Student Reviewer Interface");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        StackPane centeredPane = new StackPane(headerLabel);
        centeredPane.setMaxWidth(Double.MAX_VALUE);
        HBox header = new HBox(10, backButton, centeredPane);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        HBox.setHgrow(centeredPane, Priority.ALWAYS);
        root.setTop(header);
        
        TabPane tabPane = new TabPane();

        // browse Reviews and add reviewers
        Tab browseTab = new Tab("Browse Reviews");
        browseTab.setClosable(false);
        browseTab.setContent(createBrowseReviewsPane());

        //manage trusted reviewers
        Tab manageTab = new Tab("Trusted Reviewers");
        manageTab.setClosable(false);
        manageTab.setContent(createManageTrustedReviewersPane());

        // private feedback
        Tab feedbackTab = new Tab("Private Feedback");
        feedbackTab.setClosable(false);
        feedbackTab.setContent(createPrivateFeedbackPane());

        // request reviewer role
        Tab requestTab = new Tab("Request Reviewer Role");
        requestTab.setClosable(false);
        requestTab.setContent(createRequestReviewerPane());

        tabPane.getTabs().addAll(browseTab, manageTab, feedbackTab, requestTab);
        root.setCenter(tabPane);

        scene = new Scene(root, 900, 700);
        primaryStage.showScene(scene);
    }

    // browse reviews and add reviewer to trusted list
    private VBox createBrowseReviewsPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.setAlignment(Pos.TOP_CENTER);

        Label instructions = new Label("Select a review to view details and add the reviewer to your trusted list:");
        ListView<Review> reviewListView = new ListView<>();
        refreshAllReviews(reviewListView);

        TextArea reviewDetails = new TextArea();
        reviewDetails.setEditable(false);
        reviewDetails.setWrapText(true);
        reviewDetails.setPrefHeight(150);

        Button addReviewerButton = new Button("Add Reviewer to Trusted List");
        addReviewerButton.setOnAction(e -> {
            Review selected = reviewListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                TextInputDialog dialog = new TextInputDialog("5.0");
                dialog.setTitle("Assign Reviewer Weight");
                dialog.setHeaderText("Enter a weight (0.0 to 5.0) for the reviewer: " + selected.getReviewer());
                dialog.setContentText("Weight:");
                dialog.showAndWait().ifPresent(weightStr -> {
                    try {
                        double weight = Double.parseDouble(weightStr);
                        currentUser.addApprovedReviewer(selected.getReviewer(), weight, databaseHelper);
                        showAlert("Reviewer " + selected.getReviewer() + " added to your trusted list.");
                    } catch (NumberFormatException ex) {
                        showAlert("Invalid weight. Please enter a numeric value.");
                    }
                });
            } else {
                showAlert("Please select a review first.");
            }
        });

        // listener for selection
        reviewListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                reviewDetails.setText("Review: " + newVal.getReviewText() + "\nWritten By: " + newVal.getReviewer() + "\nLast updated: " + newVal.getTimestamp());
            }
        });

        pane.getChildren().addAll(instructions, reviewListView, reviewDetails, addReviewerButton);
        return pane;
    }

    // refresher method
    private void refreshAllReviews(ListView<Review> listView) {
        List<Review> allReviews = databaseHelper.getAllReviews();
        listView.getItems().clear();
        listView.getItems().addAll(allReviews);
    }

    private VBox createManageTrustedReviewersPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.setAlignment(Pos.TOP_CENTER);

        Label instructions = new Label("Manage your trusted reviewers and assign weights:");
        instructions.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // two columns, Reviewer and Weight
        TableView<TrustedReviewerEntry> table = new TableView<>();
        TableColumn<TrustedReviewerEntry, String> reviewerCol = new TableColumn<>("Reviewer");
        reviewerCol.setCellValueFactory(data -> data.getValue().reviewerProperty());
        TableColumn<TrustedReviewerEntry, String> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(data -> data.getValue().weightProperty());
        table.getColumns().addAll(reviewerCol, weightCol);

        // ObservableList for dynamic updates
        ObservableList<TrustedReviewerEntry> trustedReviewers = FXCollections.observableArrayList(
                TrustedReviewerEntry.fromApprovedReviewers(currentUser.getApprovedReviewers())
        );
        table.setItems(trustedReviewers);
        table.setEditable(true);
        
        // allow inline editing of the weight column.
        weightCol.setCellFactory(TextFieldTableCell.forTableColumn());
        weightCol.setOnEditCommit(event -> {
            String newWeightStr = event.getNewValue();
            try {
                double newWeight = Double.parseDouble(newWeightStr);
                String reviewerName = event.getRowValue().getReviewer();
                currentUser.updateReviewerRating(reviewerName, newWeight, databaseHelper);
                // refresh the table from the current user's approved reviewers
                trustedReviewers.setAll(TrustedReviewerEntry.fromApprovedReviewers(currentUser.getApprovedReviewers()));
            } catch (NumberFormatException ex) {
                showAlert("Invalid weight value. Please enter a numeric value.");
            }
        });
        
        // remove button to delete the selected trusted reviewer
        Button removeButton = new Button("Remove Selected Reviewer");
        removeButton.setOnAction(e -> {
            TrustedReviewerEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentUser.removeApprovedReviewer(selected.getReviewer(), databaseHelper);
                // Refresh the table list.
                trustedReviewers.setAll(TrustedReviewerEntry.fromApprovedReviewers(currentUser.getApprovedReviewers()));
            } else {
                showAlert("Please select a reviewer to remove.");
            }
        });
        
        // refresh button to reload the list
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> {
            trustedReviewers.setAll(TrustedReviewerEntry.fromApprovedReviewers(currentUser.getApprovedReviewers()));
        });
        
        HBox buttonRow = new HBox(10, refreshButton, removeButton);
        buttonRow.setAlignment(Pos.CENTER);
        
        pane.getChildren().addAll(instructions, table, buttonRow);
        return pane;
    }


    private VBox createPrivateFeedbackPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.setAlignment(Pos.TOP_CENTER);

        Label instructions = new Label("Provide private feedback to a reviewer:");
        
        // non-editable text field to display the selected reviewer
        TextField reviewerField = new TextField();
        reviewerField.setPromptText("Select a reviewer");
        reviewerField.setEditable(false);
        
        // Button to choose a reviewer from a list of usernames
        Button selectReviewerButton = new Button("Select Reviewer");
        selectReviewerButton.setOnAction(e -> {
            try {
                List<User> allUsers = databaseHelper.getUsers();
                
                // filter to the reviewer role
                List<User> reviewerUsers = allUsers.stream()
                        .filter(u -> u.getRoles() != null && u.getRoles().toLowerCase().contains("reviewer"))
                        .collect(Collectors.toList());
                if (reviewerUsers.isEmpty()) {
                    showAlert("No reviewers available.");
                    return;
                }
                // convert the list of users to usernames
                List<String> reviewerNames = reviewerUsers.stream()
                        .map(User::getUserName)
                        .collect(Collectors.toList());
                
                // create a choice dialog with the list of reviewer names
                ChoiceDialog<String> dialog = new ChoiceDialog<>(reviewerNames.get(0), reviewerNames);
                dialog.setTitle("Select Reviewer");
                dialog.setHeaderText("Choose a reviewer for your feedback:");
                dialog.setContentText("Reviewer:");
                dialog.showAndWait().ifPresent(selectedUsername -> {
                    reviewerField.setText(selectedUsername);
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Error retrieving reviewers.");
            }
        });
        
        // text area for feedback
        TextArea feedbackArea = new TextArea();
        feedbackArea.setPromptText("Enter your private feedback here...");
        feedbackArea.setPrefHeight(150);
        
        Button sendButton = new Button("Send Feedback");
        sendButton.setOnAction(e -> {
            String reviewer = reviewerField.getText().trim();
            String feedback = feedbackArea.getText().trim();
            if (reviewer.isEmpty() || feedback.isEmpty()) {
                showAlert("Reviewer and feedback must be provided.");
            } else {
                boolean success = databaseHelper.insertPrivateFeedback(currentUser.getUserName(), reviewer, feedback);
                if (success) {
                    showAlert("Feedback sent.");
                    reviewerField.clear();
                    feedbackArea.clear();
                } else {
                    showAlert("Failed to send feedback.");
                }
            }
        });
        
        HBox reviewerSelection = new HBox(10, reviewerField, selectReviewerButton);
        reviewerSelection.setAlignment(Pos.CENTER);
        
        pane.getChildren().addAll(instructions, reviewerSelection, feedbackArea, sendButton);
        return pane;
    }


    private VBox createRequestReviewerPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.setAlignment(Pos.CENTER);

        Label info = new Label("If you would like to become a reviewer, click the button below. Your request will be sent to an instructor for approval.");
        Button requestButton = new Button("Request Reviewer Role");
        requestButton.setOnAction(e -> {
            boolean success = databaseHelper.addReviewerRequest(currentUser.getUserName());
            if (success) {
                showAlert("Your request to become a reviewer has been submitted.");
                requestButton.setDisable(true);
            } else {
                showAlert("Failed to submit your request.");
            }
        });
        pane.getChildren().addAll(info, requestButton);
        return pane;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notice");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
