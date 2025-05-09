// Added by bradley

package application;

import java.sql.SQLException;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UserResetPasswordPage {
	
	private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;

    public UserResetPasswordPage(DatabaseHelper databaseHelper, QuestionManager questionManager) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
    }

	public void show(CustomTrackedStage primaryStage, User user) { // no back button here, may want to change to just username string

	    PasswordField passwordField = new PasswordField(); // create field to set password
	    passwordField.setPromptText("Enter New Password");
	    passwordField.setMaxWidth(250);
	    
	    // Label to display error messages
	    Label errorLabel = new Label();
	    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
	
	    // make a button to set password
	    // when pressed it should have a success page
	    // then force users to log in again.
	    Button setNewPasswordButton = new Button("Set New Password"); 
	    
	    setNewPasswordButton.setOnAction(a -> {
	        
	        String password = passwordField.getText();
	        
	        // check for valid password
	        PasswordRecognizer passwordTest = new PasswordRecognizer();
	        String passwordError = passwordTest.evaluatePassword(password);
	        
	        if (!passwordError.isEmpty()) {
	            errorLabel.setText("Password error: " + passwordError);
	            return;
	        }
	
			databaseHelper.changePassword(user.getUserName(), password);
	        
	        new SuccessAndLogoutPage(databaseHelper, questionManager).show(primaryStage);
	       
	    });
	
	    VBox layout = new VBox(10);
	    layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
	    layout.getChildren().addAll(passwordField, setNewPasswordButton, errorLabel);
	
	    primaryStage.showScene(new Scene(layout, 800, 400));
	    primaryStage.clearHistory(); // prevent going back
	    primaryStage.setTitle("User Login");
	    primaryStage.show();
	}

}
