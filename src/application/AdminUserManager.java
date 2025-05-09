package application;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import databasePart1.DatabaseHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AdminUserManager {

    private final DatabaseHelper databaseHelper;
    private User managedUser; // stores the selected user
    private VBox managementOverlay; // store the management overlay so we can remove/re-add it
    private VBox inviteOverlay; // field for the invite overlay
    private VBox resetPasswordOverlay; // field for the reset password overlay
    private Label messageLabel; // label for sending messages between pages
    private Boolean isWindowOpen = false; // makes sure a window is not open if the user is trying to open other menus

    public AdminUserManager(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }
    
    public void show(CustomTrackedStage primaryStage) {
        System.out.println("User " + primaryStage.getUser() + " logged in.");
        
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        Label adminLabel = new Label("User Manager Page");
        adminLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // create a table to store user list
        TableView<User> tableView = createTableView();
        tableView.setItems(getUserData(databaseHelper));
        tableView.setMaxSize(600, 250);
        
        BorderPane borderPane = new BorderPane();
        Button backButton = BackButton.createBackButton(primaryStage);
        BorderPane.setMargin(backButton, new Insets(10));
        BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
        borderPane.setTop(backButton);
        
        // for layout
        HBox manageButtons = new HBox(10);
        
        Button manageUserButton = new Button("Manage User");
        Button inviteButton = new Button("Invite");
        
        manageButtons.setAlignment(Pos.BOTTOM_CENTER);
        manageButtons.getChildren().addAll(manageUserButton, inviteButton);
        
        Label warningLabel = new Label("Please select a user");
        warningLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        warningLabel.setVisible(false);
        
        layout.getChildren().addAll(adminLabel, tableView, manageButtons, warningLabel);
        borderPane.setCenter(layout);
        
        StackPane root = new StackPane(borderPane);
        
        managementOverlay = createManagementOverlayPane(primaryStage, tableView, root);
        managementOverlay.setVisible(false);
        root.getChildren().add(managementOverlay);

        // create the invite overlay once and hide it and add it to the root
        inviteOverlay = inviteUserCustomPane();
        inviteOverlay.setVisible(false);
        root.getChildren().add(inviteOverlay);

        // set the invite button shows the invite overlay
        inviteButton.setOnAction(a -> {
        	if (isWindowOpen) return;
            inviteOverlay.setVisible(true);
        	isWindowOpen = true;
        });
        
        manageUserButton.setOnAction(a -> {
        	if (isWindowOpen) return;
            managedUser = tableView.getSelectionModel().getSelectedItem();
            if (managedUser == null) {
                warningLabel.setVisible(true);
            } else {
            	isWindowOpen = true;
                warningLabel.setVisible(false);
                messageLabel.setText("");
                // if the management overlay was removed after deleting user, add it back
                if (!root.getChildren().contains(managementOverlay)) {
                	isWindowOpen = true;
                    root.getChildren().add(managementOverlay);
                }
                showManagementOverlay(managementOverlay, managedUser);
            }
        });

        Scene userManagerScene = new Scene(root, 800, 400);
        primaryStage.setTitle("User Manager");
        primaryStage.showScene(userManagerScene);
    }
    
    // helper method to decrease amount of code in main block. We can use this similar to our main stage show
    private void showManagementOverlay(VBox overlayPane, User user) {
        System.out.println("Managing " + user + ".");
        Label userInfoLabel = (Label) overlayPane.getChildren().get(0);
        userInfoLabel.setText("Managing user: " + user.getUserName());
        overlayPane.setVisible(true);
        isWindowOpen = true;
    }
    
    private VBox createManagementOverlayPane(CustomTrackedStage primaryStage, TableView<User> tableView, StackPane root) {
        VBox overlayPane = new VBox(10);
        overlayPane.setStyle(
            "-fx-background-color: rgba(184, 184, 184, .95); " +
            "-fx-padding: 20; " +
            "-fx-background-radius: 15; " +
            "-fx-border-radius: 15; " +
            "-fx-border-color: black; " +
            "-fx-border-width: 2;"
        );
        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.setMaxSize(400, 300);
        overlayPane.setPadding(new Insets(20));
        
        Label userInfoLabel = new Label();
        userInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        Button editRoleButton = new Button("Edit Roles");
        Button resetPasswordButton = new Button("Reset Password");
        Button deleteButton = new Button("Delete User");
        
        HBox buttonLayout = new HBox(10);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.getChildren().addAll(editRoleButton, resetPasswordButton, deleteButton);
        
       	messageLabel = new Label();
       	messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        Button closeButton = new Button("Close");
        closeButton.setAlignment(Pos.BOTTOM_CENTER);
        
        editRoleButton.setOnAction(e -> {
            if (managedUser == null) {
            	messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
            	messageLabel.setText("Please select a user to edit roles.");
                System.out.println("Please select a user to edit roles.");
            } else {
                User adminUser = primaryStage.getUser();
                VBox roleOverlay = createRoleSelectionOverlayPane(primaryStage, managedUser, adminUser, root, tableView);
                root.getChildren().add(roleOverlay);
            	isWindowOpen = true; // should already be true but just to make sure
            	messageLabel.setText("");
            }
        });
        
        
        deleteButton.setOnAction(e -> {
            if (managedUser == null) {
                System.out.println("No user selected.");
                messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                messageLabel.setText("No user selected.");
                return;
            }
            if (managedUser.getUserName().equals(primaryStage.getUser().getUserName())) {
                System.out.println("Cannot delete self.");
                messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                messageLabel.setText("Cannot delete self.");
            } else {
                VBox deleteOverlay = deleteUserConfirmationOverlayPane(tableView, managedUser, root);
                root.getChildren().add(deleteOverlay);
                isWindowOpen = false; // close windows
                messageLabel.setText("");
            }
        });
        
        resetPasswordButton.setOnAction(e -> {
            if (managedUser == null) {
                System.out.println("No user selected.");
                messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                messageLabel.setText("No user selected.");
                return;
            }
            
            resetPasswordOverlay = resetPasswordOverlayPane(managedUser.getUserName(), root);
            root.getChildren().add(resetPasswordOverlay);
        	isWindowOpen = true; // should already be true but just to make sure
        	messageLabel.setText("");
        });
        
        closeButton.setOnAction(e -> {
	        overlayPane.setVisible(false);
	        isWindowOpen = false;
            messageLabel.setText("");
        });
        
        overlayPane.getChildren().addAll(userInfoLabel, buttonLayout, messageLabel, closeButton);
        return overlayPane;
    }
    
    
    private VBox createRoleSelectionOverlayPane(CustomTrackedStage primaryStage, User userToUpdate, User adminUser, StackPane root, TableView<User> tableView) {
        VBox roleOverlayPane = new VBox(10);
        roleOverlayPane.setStyle(
            "-fx-background-color: rgba(184, 184, 184, .95); " +
            "-fx-padding: 20; " +
            "-fx-background-radius: 15; " +
            "-fx-border-radius: 15; " +
            "-fx-border-color: black; " +
            "-fx-border-width: 2;"
        );
        roleOverlayPane.setAlignment(Pos.CENTER);
        roleOverlayPane.setMaxSize(400, 300);
        roleOverlayPane.setPadding(new Insets(20));
        
        Label userInfoLabel = new Label("Change " + userToUpdate.getUserName() + "'s roles:");
        userInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        CheckBox adminCheck = new CheckBox("Admin");
        CheckBox studentCheck = new CheckBox("Student");
        CheckBox instructorCheck = new CheckBox("Instructor");
        CheckBox reviewerCheck = new CheckBox("Reviewer");
        CheckBox staffCheck = new CheckBox("Staff");
        
        adminCheck.setSelected(databaseHelper.hasRole(userToUpdate.getUserName(), "Admin"));
        studentCheck.setSelected(databaseHelper.hasRole(userToUpdate.getUserName(), "Student"));
        instructorCheck.setSelected(databaseHelper.hasRole(userToUpdate.getUserName(), "Instructor"));
        reviewerCheck.setSelected(databaseHelper.hasRole(userToUpdate.getUserName(), "Reviewer"));
        staffCheck.setSelected(databaseHelper.hasRole(userToUpdate.getUserName(), "Staff"));
        
        VBox checkBoxContainer = new VBox(5);
        checkBoxContainer.setAlignment(Pos.CENTER_LEFT);
        checkBoxContainer.getChildren().addAll(adminCheck, studentCheck, instructorCheck, reviewerCheck, staffCheck);
        
        Button cancelButton = new Button("Cancel");
        Button saveButton = new Button("Save");
        
        HBox editRoleLayout = new HBox(10, cancelButton, saveButton);
        editRoleLayout.setAlignment(Pos.BOTTOM_CENTER);
        
        cancelButton.setOnAction(e -> root.getChildren().remove(roleOverlayPane));
        
        // functions to edit user roles and call database methods
        saveButton.setOnAction(e -> {
            if (adminCheck.isSelected()) {
                databaseHelper.addUserRole(userToUpdate.getUserName(), "Admin");
            } else {
                databaseHelper.removeUserRole(userToUpdate.getUserName(), "Admin");
            }
            if (studentCheck.isSelected()) {
                databaseHelper.addUserRole(userToUpdate.getUserName(), "Student");
            } else {
                databaseHelper.removeUserRole(userToUpdate.getUserName(), "Student");
            }
            if (instructorCheck.isSelected()) {
                databaseHelper.addUserRole(userToUpdate.getUserName(), "Instructor");
            } else {
                databaseHelper.removeUserRole(userToUpdate.getUserName(), "Instructor");
            }
            if (reviewerCheck.isSelected()) {
                databaseHelper.addUserRole(userToUpdate.getUserName(), "Reviewer");
            } else {
                databaseHelper.removeUserRole(userToUpdate.getUserName(), "Reviewer");
            }
            if (staffCheck.isSelected()) {
                databaseHelper.addUserRole(userToUpdate.getUserName(), "Staff");
                databaseHelper.addUserRole(userToUpdate.getUserName(), "Reviewer"); // automatically also give the reviewer role
            } else {
                databaseHelper.removeUserRole(userToUpdate.getUserName(), "Staff");
            }
			try {
				managedUser = databaseHelper.getUser(userToUpdate.getUserName());
				System.out.println("User roles are " + managedUser.getRoles());
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			// ensure we have at least 1 admin
			if (databaseHelper.getAdminCount() < 1) {
                databaseHelper.addUserRole(userToUpdate.getUserName(), "Admin");
                messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                messageLabel.setText("Must have at least one admin user");
				System.out.println("Must have at least one admin user");
			}
            
            if (primaryStage.getUser().getUserName().equals(managedUser.getUserName())) {
            	// update active user object with permissions and set as active user
            	primaryStage.setUser(managedUser);
				System.out.println("Primay user is " + primaryStage.getUser().getUserName());
            }
            
            tableView.setItems(getUserData(databaseHelper));
            root.getChildren().remove(roleOverlayPane);
        });
        
        roleOverlayPane.getChildren().addAll(userInfoLabel, checkBoxContainer, editRoleLayout);
        return roleOverlayPane;
    }

    // overlay
    private VBox deleteUserConfirmationOverlayPane(TableView<User> tableView, User userToUpdate, StackPane root) {
        VBox deleteOverlayPane = new VBox(10);
        deleteOverlayPane.setStyle(
            "-fx-background-color: rgba(184, 184, 184, .95); " +
            "-fx-padding: 20; " +
            "-fx-background-radius: 15; " +
            "-fx-border-radius: 15; " +
            "-fx-border-color: black; " +
            "-fx-border-width: 2;"
        );
        deleteOverlayPane.setAlignment(Pos.CENTER);
        deleteOverlayPane.setMaxSize(400, 300);
        deleteOverlayPane.setPadding(new Insets(20));
        
        Label userInfoLabel = new Label("Delete " + userToUpdate.getUserName() + ".");
        userInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        Label warningLabel = new Label("Are you sure? This action cannot be undone.");
        warningLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        Button cancelButton = new Button("Cancel");
        Button deleteButton = new Button("Delete");
        
        HBox deleteUserLayout = new HBox(10);
        deleteUserLayout.setAlignment(Pos.BOTTOM_CENTER);
        deleteUserLayout.getChildren().addAll(cancelButton, deleteButton);
        
        cancelButton.setOnAction(e -> root.getChildren().remove(deleteOverlayPane));
        
        deleteButton.setOnAction(e -> {
            System.out.println("Deleted user " + userToUpdate.getUserName());
            if (databaseHelper.deleteUser(managedUser.getUserName())) {
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("Failed to delete user.");
            }
            tableView.setItems(getUserData(databaseHelper));
            root.getChildren().remove(deleteOverlayPane);
            root.getChildren().remove(managementOverlay);
            managedUser = null;
            isWindowOpen = false;
        });
        
        deleteOverlayPane.getChildren().addAll(userInfoLabel, warningLabel, deleteUserLayout);
        return deleteOverlayPane;
    }
    
    // custom pane view to display invite code. 
    private VBox inviteUserCustomPane() {
        VBox layout = new VBox(10);
        layout.setStyle(
            "-fx-background-color: rgba(184, 184, 184, .95); " +
            "-fx-padding: 20; " +
            "-fx-background-radius: 15; " +
            "-fx-border-radius: 15; " +
            "-fx-border-color: black; " +
            "-fx-border-width: 2;"
        );
        layout.setAlignment(Pos.CENTER);
        layout.setMaxSize(400, 300);
        layout.setPadding(new Insets(20));
        
        // Title Label
        Label titleLabel = new Label("Invite");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Button to generate invitation code
        Button showCodeButton = new Button("Generate Invitation Code");
        
        // Label to display the generated code
        Label inviteCodeLabel = new Label("");
        inviteCodeLabel.setStyle("-fx-font-size: 14px; -fx-font-style: italic;");
        
        showCodeButton.setOnAction(a -> {
            String invitationCode = databaseHelper.generateInvitationCode();
            inviteCodeLabel.setText(invitationCode);
        });
        
        // Done button simply hides the invite overlay
        Button doneButton = new Button("Done");
        doneButton.setOnAction(e -> {
            inviteCodeLabel.setText("");
            inviteOverlay.setVisible(false);
            isWindowOpen = false;
        });
        
        layout.getChildren().addAll(titleLabel, showCodeButton, inviteCodeLabel, doneButton);
        return layout;
    }
    
    // pass in methods to know the user to update
    private VBox resetPasswordOverlayPane(String userToUpdateUsername, StackPane root) {
        VBox layout = new VBox(10);
        layout.setStyle(
            "-fx-background-color: rgba(184, 184, 184, .95); " +
            "-fx-padding: 20; " +
            "-fx-background-radius: 15; " +
            "-fx-border-radius: 15; " +
            "-fx-border-color: black; " +
            "-fx-border-width: 2;"
        );
        layout.setAlignment(Pos.CENTER);
        layout.setMaxSize(400, 300);
        layout.setPadding(new Insets(20));
        
	    Label passLabel = new Label("Set one time password for " + userToUpdateUsername);
		passLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
	
		TextField tempPasswordField = new TextField();
		tempPasswordField.setPromptText("Enter temporary password");
		tempPasswordField.setMaxWidth(250);
	
		Button setOTPButton = new Button("Set OTP");
		Button cancelButton = new Button("Cancel");

		setOTPButton.setOnAction(a -> {
	
			String tempPassword = tempPasswordField.getText();
	
			// ensure user exists
			if (!databaseHelper.doesUserExist(userToUpdateUsername)) {
				return;
			}
	
			databaseHelper.addUserOTP(userToUpdateUsername, tempPassword);
	
			System.out.println("OTP password for " + userToUpdateUsername + " set to " + tempPassword + ".");
            root.getChildren().remove(resetPasswordOverlay);
            
            resetPasswordOverlay.setVisible(false);
            messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            messageLabel.setText("OTP password for " + userToUpdateUsername + " set to " + tempPassword + ".");
		});
		
		cancelButton.setOnAction(a ->{
			root.getChildren().remove(resetPasswordOverlay);
            messageLabel.setText("");
            resetPasswordOverlay.setVisible(false);			
		});
		
		HBox submitButtons = new HBox(10);
		submitButtons.setAlignment(Pos.BOTTOM_CENTER);
		submitButtons.getChildren().addAll(cancelButton, setOTPButton);
		
        layout.getChildren().addAll(passLabel, tempPasswordField, submitButtons);
        return layout;
    }
    
    // table to dispaly all user data
    private TableView<User> createTableView() {
        TableView<User> tableView = new TableView<>();

        TableColumn<User, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserName()));
        usernameColumn.setMinWidth(150);

        TableColumn<User, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        emailColumn.setMinWidth(200);

        TableColumn<User, String> roleColumn = new TableColumn<>("Role");
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoles()));
        roleColumn.setMinWidth(150);

        tableView.getColumns().addAll(Arrays.asList(usernameColumn, emailColumn, roleColumn));
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        return tableView;
    }

    // observable list to update data in real time
    private ObservableList<User> getUserData(DatabaseHelper databaseHelper) {
        try {
            return FXCollections.observableArrayList(databaseHelper.getUsers());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return FXCollections.observableArrayList(new ArrayList<User>());
    }
}
