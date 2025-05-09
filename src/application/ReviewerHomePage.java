package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class ReviewerHomePage {

    private Scene scene;
    private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;
    private final User currentUser;
    private ListView<Review> reviewsListView;

    public ReviewerHomePage(DatabaseHelper databaseHelper, QuestionManager questionManager, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
        this.currentUser = currentUser;
    }

    public void show(CustomTrackedStage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f9f9f9;");

        HBox header = new HBox(10);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER_LEFT);
        Button backButton = BackButton.createBackButton(primaryStage);
        Label titleLabel = new Label("Reviewer Dashboard");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button viewProfileButton = new Button("View Profile");
        viewProfileButton.setOnAction(e -> {
            new ReviewerProfilePage(databaseHelper, currentUser).show(primaryStage);
        });
        header.getChildren().addAll(backButton, titleLabel, spacer, viewProfileButton);
        root.setTop(header);
        
        Button inboxButton = new Button("Review Feedback Inbox");
        inboxButton.setOnAction(e -> {
            new ReviewerInboxPage(databaseHelper, questionManager, currentUser).show(primaryStage);
        });

        // store list of reviewers to manage
        reviewsListView = new ListView<>();
        refreshReviewsList();
        root.setCenter(reviewsListView);

        HBox bottom = new HBox(10);
        bottom.setPadding(new Insets(10));
        bottom.setAlignment(Pos.CENTER);
        Button createReviewButton = new Button("Create New Review");
        createReviewButton.setOnAction(e -> {
            new CreateReviewPage(databaseHelper, questionManager, currentUser, this::refreshReviewsList)
                .show(primaryStage);
        });
        Button updateReviewButton = new Button("Update Selected Review");
        updateReviewButton.setOnAction(e -> {
            Review selected = reviewsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new UpdateReviewPage(databaseHelper, currentUser, selected, this::refreshReviewsList)
                    .show(primaryStage);
            } else {
                showAlert("Please select a review to update.");
            }
        });
        bottom.getChildren().addAll(createReviewButton, updateReviewButton, inboxButton);
        root.setBottom(bottom);

        scene = new Scene(root, 900, 700);
        primaryStage.showScene(scene);
    }

    // refresh the review list
    private void refreshReviewsList() {
        List<Review> reviews = databaseHelper.getReviewsByReviewer(currentUser.getUserName());
        reviewsListView.getItems().clear();
        reviewsListView.getItems().addAll(reviews);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
