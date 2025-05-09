package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

import databasePart1.*;

/**
 * The SetupAdmin class handles the setup process for creating an administrator account.
 * This is intended to be used by the first user to initialize the system with admin credentials.
 */
public class AdminSetupPage {
	
	private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;


    public AdminSetupPage(DatabaseHelper databaseHelper, QuestionManager questionManager) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
    }

    public void show(CustomTrackedStage primaryStage) {
    	// Input fields for userName and password
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter Admin userName");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
       
        Button setupButton = new Button("Setup");
        
        setupButton.setOnAction(a -> {
        	// Retrieve user input
            
            String userName = userNameField.getText();
            UserNameRecognizer userTest = new UserNameRecognizer();
            String userError = userTest.checkForValidUserName(userName); // validate the username using custom methods
            
            String password = passwordField.getText();
            PasswordRecognizer passwordTest = new PasswordRecognizer();
            String passwordError = passwordTest.evaluatePassword(password); // validate the password using custom methods
            
            if (!userError.isEmpty() || !passwordError.isEmpty()) {
                errorLabel.setText("Username error: " + userError + ". Password error: " + passwordError);
                return;
            }
            
            if (!userError.isEmpty() || !passwordError.isEmpty()) {
                System.out.println("Username error: " + userError + ". Password error: " + passwordError);
                return;
            }
            
            try {
            	// Create a new User object with admin role and register in the database
            	User user=new User(userName, password, "Admin, Student, Instructor");
                databaseHelper.register(user);
                System.out.println("Administrator setup completed.");
	            
                // Navigate to the Welcome Login Page
                new WelcomeLoginPage(databaseHelper, questionManager).show(primaryStage, user);
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

        VBox layout = new VBox(10, userNameField, passwordField, setupButton, errorLabel);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

		borderPane.setCenter(layout);
		Scene adminScene = new Scene(borderPane, 800, 400); 
		
		primaryStage.showScene(adminScene);		
		primaryStage.setTitle("Administrator Setup");
        primaryStage.show();
    }
}
