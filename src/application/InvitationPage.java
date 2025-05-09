package application;


import databasePart1.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class InvitationPage {

	/**
     * Displays the Invite Page in the provided primary stage.
     * 
     * @param databaseHelper An instance of DatabaseHelper to handle database operations.
     * @param primaryStage   The primary stage where the scene will be displayed.
     */
    public void show(DatabaseHelper databaseHelper,CustomTrackedStage primaryStage) {
    	VBox layout = new VBox();
	    layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
	    
	    // Label to display the title of the page
	    Label userLabel = new Label("Invite ");
	    userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
	    
	    // Button to generate the invitation code
	    Button showCodeButton = new Button("Generate Invitation Code");
	    
	    // Label to display the generated invitation code
	    Label inviteCodeLabel = new Label(""); ;
        inviteCodeLabel.setStyle("-fx-font-size: 14px; -fx-font-style: italic;");
        
        showCodeButton.setOnAction(a -> {
        	// Generate the invitation code using the databaseHelper and set it to the label
            String invitationCode = databaseHelper.generateInvitationCode();
            inviteCodeLabel.setText(invitationCode);
        });
	    
        BorderPane borderPane = new BorderPane();

        Button backButton = BackButton.createBackButton(primaryStage); // premade button style

        BorderPane.setMargin(backButton, new Insets(10));  // adds padding outside the button
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        borderPane.setTop(backButton);

        layout.getChildren().addAll(userLabel, showCodeButton, inviteCodeLabel);

        borderPane.setCenter(layout);
        Scene inviteScene = new Scene(borderPane, 800, 400);
        
	    // Set the scene to primary stage
	    primaryStage.showScene(inviteScene);
	    primaryStage.setTitle("Invite Page");
    	
    }
}