package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

import databasePart1.*;

/**
 * The UserLoginPage class provides a login interface for users to access their accounts.
 * It validates the user's credentials and navigates to the appropriate page upon successful login.
 */
public class UserLoginPage {
	
    private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;

    public UserLoginPage(DatabaseHelper databaseHelper, QuestionManager questionManager) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
    }

    public void show(CustomTrackedStage primaryStage) {
    	
    	System.out.println("User " + primaryStage.getUser() + " logged in.");
    	
        BorderPane borderPane = new BorderPane();

        Button backButton = BackButton.createBackButton(primaryStage);

        BorderPane.setMargin(backButton, new Insets(10));  // adds padding outside the button
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        borderPane.setTop(backButton);
        
    	// Input field for the user's userName, password
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter User Name");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);
        
        // Label to display error messages
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        Button loginButton = new Button("Login");
        
        loginButton.setOnAction(a -> {
        	
            String userName = userNameField.getText();
            String password = passwordField.getText();
        	
        	/* no syntax checks needed if you are logging in */
            
            try {
            	User user = new User(userName, password, "");
            	WelcomeLoginPage welcomeLoginPage = new WelcomeLoginPage(databaseHelper, questionManager);
            	
            	// retrieve the user's role from the database using userName
            	String roles = databaseHelper.getUserRoles(userName);
            	
            	if(roles!=null) {
            		user.setRoles(roles);
            		
        			if (databaseHelper.validateUserOTP(userName, password)) { // user has an OTP active
        			
            			primaryStage.setUser(user); // set the user of the application
            			System.out.println("User " + user.getUserName() + " logged in using OTP.");
            			
            			new UserResetPasswordPage(databaseHelper, questionManager).show(primaryStage, user);
        			}else if (databaseHelper.login(user)) {
            			primaryStage.setUser(user); // set the user of the application
            			System.out.println("User " + user.getUserName() + " logged in.");
            			
            			if (databaseHelper.getRoleCount(user.getUserName()) > 1) {
                			welcomeLoginPage.show(primaryStage,user);
            			}else {
            				String role = databaseHelper.getUserRoles(user.getUserName());
   
            				// find which pages the user can access
            				if (role.contains("Student")) {
                            	new StudentHomePage(databaseHelper, questionManager, user).show(primaryStage);
            				} else if (role.contains("Admin")) {
                                new AdminHomePage(databaseHelper).show(primaryStage);
            				} else if (role.contains("Instructor")) {
                                new InstructorHomePage(databaseHelper, user, questionManager).show(primaryStage);
            				} else if (role.contains("Staff")) {
                                new StaffHomePage(databaseHelper, user).show(primaryStage, questionManager);
            				} else if (role.contains("Reviewer")) {
                                new ReviewerHomePage(databaseHelper, questionManager, user).show(primaryStage);
            				}
            			}
            		} else {
            			primaryStage.setUser(null); // set the user of the application
            			System.out.println("User set to NULL.");
            			// Display an error if the login fails
                        errorLabel.setText("Error logging in");
            		}
            	} else {
            		// Display an error if the account does not exist
                    errorLabel.setText("user account doesn't exists");
            	}
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            } 
        });

        VBox centerLayout = new VBox(10);
        centerLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        centerLayout.getChildren().addAll(userNameField, passwordField, loginButton, errorLabel);

        borderPane.setCenter(centerLayout);
        
        Scene scene = new Scene(borderPane, 800, 400);
        primaryStage.showScene(scene);
        primaryStage.setTitle("User Login");
        primaryStage.show();
    }
}
