// Added by bradley, but changed implementation so we don't need it anymore

package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;


public class AdminOTPPage {

	private final DatabaseHelper databaseHelper;

	public AdminOTPPage(DatabaseHelper databaseHelper) {
		this.databaseHelper = databaseHelper;
	}

	
	public void show(CustomTrackedStage primaryStage) {
		
    	System.out.println("User " + primaryStage.getUser() + " logged in.");

		VBox layout = new VBox();

		layout.setStyle("-fx-alignment: center; -fx-padding: 20;");

		Label pageLabel = new Label("Set User OTP");

		pageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

		Label usernameLabel = new Label("Username");
		usernameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

		TextField userNameField = new TextField();
		userNameField.setPromptText("Enter username");
		userNameField.setMaxWidth(250);

		Label passLabel = new Label("Set one time password");
		passLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

		TextField tempPasswordField = new TextField();
		tempPasswordField.setPromptText("Enter temporary password");
		tempPasswordField.setMaxWidth(250);

		Button setOTPButton = new Button("Set OTP");

		setOTPButton.setOnAction(a -> {

			String userName = userNameField.getText();
			String tempPassword = tempPasswordField.getText();

			if (!databaseHelper.doesUserExist(userName)) {
				return;
			}

			databaseHelper.addUserOTP(userName, tempPassword);

			//new SuccessAndLogoutPage(databaseHelper).show(primaryStage, otpPassword);

		});
		
		BorderPane borderPane = new BorderPane();

        Button backButton = BackButton.createBackButton(primaryStage); // premade button style
        
        BorderPane.setMargin(backButton, new Insets(10));  // adds padding outside the button
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        borderPane.setTop(backButton);

		layout.getChildren().addAll(pageLabel, usernameLabel, userNameField, passLabel, tempPasswordField,
				setOTPButton);
		borderPane.setCenter(layout);
		Scene adminScene = new Scene(borderPane, 800, 400);

		// show the scene to primary stage (custom function)
		primaryStage.showScene(adminScene);
		primaryStage.setTitle("Admin OTP Setup");
	}
}
