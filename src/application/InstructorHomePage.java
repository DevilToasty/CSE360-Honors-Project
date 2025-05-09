package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * StudentHomePage class represents the user interface for the student user.
 * This page includes only a back button to return to the previous page.
 */
public class InstructorHomePage {

    private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;
    private final User currentUser;

    public InstructorHomePage(DatabaseHelper databaseHelper, User currentUser, QuestionManager questionManager) {
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
        this.questionManager = questionManager;
    }

    public void show(CustomTrackedStage primaryStage) {
        BorderPane borderPane = new BorderPane();

        // Back Button at the top left
        Button backButton = new Button("<-- Back");
        backButton.setOnAction(e -> primaryStage.goBack());
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        borderPane.setTop(backButton);
        Label userLabel = new Label("Hello, Instructor!");
        userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // main page
        Button manageButton = new Button("Manage");
        manageButton.setOnAction(e -> {
        	new InstructorReviewerManagerPage(databaseHelper, questionManager, currentUser).show(primaryStage);
        });

        VBox centerBox = new VBox(userLabel, manageButton);
        centerBox.setAlignment(Pos.CENTER);
        borderPane.setCenter(centerBox);

        Scene scene = new Scene(borderPane, 800, 400);
        primaryStage.setTitle("Instructor Home");
        primaryStage.showScene(scene);
    }
}