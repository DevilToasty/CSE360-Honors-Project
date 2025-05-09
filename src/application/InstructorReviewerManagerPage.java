package application;

import databasePart1.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class InstructorReviewerManagerPage {

    private Scene scene;
    private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;
    private final User currentUser;

    public InstructorReviewerManagerPage(DatabaseHelper databaseHelper, QuestionManager questionManager, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
        this.currentUser = currentUser;
    }

    public void show(CustomTrackedStage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f9f9f9;");

        Button backButton = BackButton.createBackButton(primaryStage);
        Label titleLabel = new Label("Instructor Reviewer Management");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        BorderPane headerPane = new BorderPane();
        headerPane.setLeft(backButton);
        headerPane.setCenter(titleLabel);
        BorderPane.setAlignment(backButton, Pos.CENTER_LEFT);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        headerPane.setPadding(new Insets(10));
        root.setTop(headerPane);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Student Posts
        Tab studentPostsTab = new Tab("Student Posts");
        studentPostsTab.setContent(createStudentPostsPane());

        // Tab 2: Reviewer Requests
        Tab reviewerRequestsTab = new Tab("Reviewer Requests");
        reviewerRequestsTab.setContent(createReviewerRequestsPane());

        tabPane.getTabs().addAll(studentPostsTab, reviewerRequestsTab);
        root.setCenter(tabPane);

        scene = new Scene(root, 1000, 700);
        primaryStage.showScene(scene);
    }

    // 
    private VBox createStudentPostsPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));
        pane.setAlignment(Pos.TOP_CENTER);

        Label selectLabel = new Label("Select a student to view their posts:");
        selectLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ComboBox<User> studentCombo = new ComboBox<>();
        try {
            List<User> allUsers = databaseHelper.getUsers();
            List<User> students = allUsers.stream()
                    .filter(u -> u.getRoles() != null && u.getRoles().toLowerCase().contains("student"))
                    .collect(Collectors.toList());
            studentCombo.setItems(FXCollections.observableArrayList(students));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        studentCombo.setPromptText("Select a student");

        // custom converter so that it displays the student's name
        studentCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getUserName();
            }
            @Override
            public User fromString(String string) {
                return null;  // Not needed
            }
        });

        studentCombo.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? "" : user.getUserName());
            }
        });

        // student's questions and answers.
        Label questionsLabel = new Label("Questions:");
        ListView<Question> questionsList = new ListView<>();
        questionsList.setPrefHeight(200);
        Label answersLabel = new Label("Answers:");
        ListView<Answer> answersList = new ListView<>();
        answersList.setPrefHeight(200);

        // when a student is selected load their posts
        studentCombo.setOnAction(e -> {
            User selectedStudent = studentCombo.getSelectionModel().getSelectedItem();
            if (selectedStudent != null) {
                List<Question> studentQuestions = questionManager.findQuestionsByAuthor(selectedStudent.getUserName());
                List<Answer> studentAnswers = questionManager.findAnswersByAuthor(selectedStudent.getUserName());
                questionsList.setItems(FXCollections.observableArrayList(studentQuestions));
                answersList.setItems(FXCollections.observableArrayList(studentAnswers));
            }
        });

        pane.getChildren().addAll(selectLabel, studentCombo, questionsLabel, questionsList, answersLabel, answersList);
        return pane;
    }

    private VBox createReviewerRequestsPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(15));
        pane.setAlignment(Pos.TOP_CENTER);

        Label header = new Label("Pending Reviewer Requests:");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // display pending reviewer requests
        ListView<ReviewerRequest> requestList = new ListView<>();
        refreshReviewerRequests(requestList);

        // Approve and Deny buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        Button approveButton = new Button("Approve");
        Button denyButton = new Button("Deny");

        approveButton.setOnAction(e -> {
            ReviewerRequest selected = requestList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Approve the request:
                boolean updated = databaseHelper.updateReviewerRequest(selected.getUsername(), true);
                if (updated) {
                    // add the Reviewer role to that student
                    databaseHelper.addUserRole(selected.getUsername(), "Reviewer");
                    showAlert("Request approved and Reviewer role granted to " + selected.getUsername());
                    refreshReviewerRequests(requestList);
                } else {
                    showAlert("Failed to approve the request.");
                }
            } else {
                showAlert("Please select a request to approve.");
            }
        });

        denyButton.setOnAction(e -> {
            ReviewerRequest selected = requestList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // deny the request (isApproved false)
                boolean updated = databaseHelper.updateReviewerRequest(selected.getUsername(), false);
                if (updated) {
                    showAlert("Request denied for " + selected.getUsername());
                    refreshReviewerRequests(requestList);
                } else {
                    showAlert("Failed to deny the request.");
                }
            } else {
                showAlert("Please select a request to deny.");
            }
        });
        buttonBox.getChildren().addAll(approveButton, denyButton);

        pane.getChildren().addAll(header, requestList, buttonBox);
        return pane;
    }

    // update list
    private void refreshReviewerRequests(ListView<ReviewerRequest> listView) {
        List<ReviewerRequest> requests = databaseHelper.getReviewerRequests();
        listView.getItems().clear();
        listView.getItems().addAll(requests);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Instructor Manager");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
