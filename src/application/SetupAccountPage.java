package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.sql.SQLException;

import databasePart1.*;

public class SetupAccountPage {

	private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;

    public SetupAccountPage(DatabaseHelper databaseHelper, QuestionManager questionManager) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
    }

    public void show(CustomTrackedStage primaryStage) {
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter userName");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);

        TextField inviteCodeField = new TextField();
        inviteCodeField.setPromptText("Enter Invitation Code");
        inviteCodeField.setMaxWidth(250);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        Button setupButton = new Button("Setup");

        setupButton.setOnAction(a -> {

            String code = inviteCodeField.getText();
            String userName = userNameField.getText();
            String userError = UserNameRecognizer.checkForValidUserName(userName);
            String password = passwordField.getText();
            String passwordError = PasswordRecognizer.evaluatePassword(password);

            // ensure username and password are valid
            if (!userError.isEmpty() || !passwordError.isEmpty()) {
                errorLabel.setText("Username error: " + userError + ". Password error: " + passwordError);
                return;
            }

            try {
                if (!databaseHelper.doesUserExist(userName)) {
                    if (databaseHelper.validateInvitationCode(code)) {
                        User user = new User(userName, password, "Student");
                        databaseHelper.register(user);
                        new WelcomeLoginPage(databaseHelper, questionManager).show(primaryStage, user);
                    } else {
                        errorLabel.setText("Please enter a valid invitation code");
                    }
                } else {
                    errorLabel.setText("This username is taken! Please choose another.");
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        BorderPane borderPane = new BorderPane();

        Button backButton = BackButton.createBackButton(primaryStage); // premade button style

        BorderPane.setMargin(backButton, new Insets(10));  // adds padding outside the button
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        borderPane.setTop(backButton);

        VBox centerLayout = new VBox(10);
        centerLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        centerLayout.getChildren().addAll(userNameField, passwordField, inviteCodeField, setupButton, errorLabel);
        borderPane.setCenter(centerLayout);

        Scene scene = new Scene(borderPane, 800, 400);
        primaryStage.setTitle("Account Setup");
        primaryStage.showScene(scene);
    }
}
