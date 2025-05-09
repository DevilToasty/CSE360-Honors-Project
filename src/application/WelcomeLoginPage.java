package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.geometry.Pos;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import databasePart1.*;

/**
 * The WelcomeLoginPage class displays a welcome screen for authenticated users.
 * It allows users to navigate to their respective pages based on their role or quit the application.
 */
public class WelcomeLoginPage {

	private final DatabaseHelper databaseHelper;
    private final QuestionManager questionManager;

    public WelcomeLoginPage(DatabaseHelper databaseHelper, QuestionManager questionManager) {
        this.databaseHelper = databaseHelper;
        this.questionManager = questionManager;
    }

    public void show(CustomTrackedStage primaryStage, User user) {
        User updatedUser = null;
        try {
            updatedUser = databaseHelper.getUser(user.getUserName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        primaryStage.setUser(updatedUser);
        String username = updatedUser.getUserName();
        System.out.println("User " + username + " logged in.");

        VBox radioBox = new VBox(10);
        radioBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(5);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");

        Label welcomeLabel = new Label("Select your role:");
        welcomeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // group of all role selection options
        ToggleGroup group = new ToggleGroup();

        if (databaseHelper.hasRole(username, "Admin")) {
            System.out.println(username + " has Admin role.");
            RadioButton adminButton = new RadioButton("Admin");
            adminButton.setToggleGroup(group);
            radioBox.getChildren().add(adminButton);
        }
        if (databaseHelper.hasRole(username, "Student")) {
            System.out.println(username + " has Student role.");
            RadioButton studentButton = new RadioButton("Student");
            studentButton.setToggleGroup(group);
            radioBox.getChildren().add(studentButton);
        }
        if (databaseHelper.hasRole(username, "Instructor")) {
            System.out.println(username + " has Instructor role.");
            RadioButton instructorButton = new RadioButton("Instructor");
            instructorButton.setToggleGroup(group);
            radioBox.getChildren().add(instructorButton);
        }
        if (databaseHelper.hasRole(username, "Reviewer")) {
            System.out.println(username + " has Reviewer role.");
            RadioButton reviewerButton = new RadioButton("Reviewer");
            reviewerButton.setToggleGroup(group);
            radioBox.getChildren().add(reviewerButton);
        }
        if (databaseHelper.hasRole(username, "Staff")) {
            System.out.println(username + " has Staff role.");
            RadioButton staffButton = new RadioButton("Staff");
            staffButton.setToggleGroup(group);
            radioBox.getChildren().add(staffButton);
        }

        // if they have no roles (shouldn't ever happen)
        if (radioBox.getChildren().isEmpty()) {
            System.out.println("No roles found for user " + username + ". Adding default role.");
            RadioButton defaultButton = new RadioButton("No Role");
            defaultButton.setToggleGroup(group);
            radioBox.getChildren().add(defaultButton);
        }

        if (!group.getToggles().isEmpty()) {
            group.selectToggle(group.getToggles().get(0));
        }

        Button continueButton = new Button("Continue to your Page");
        continueButton.setOnAction(a -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            if (selected == null) {
                System.out.println("No role selected.");
                return;
            }
            String role = selected.getText();
            System.out.println("Selected role: " + role);
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
        });

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(a -> {
            primaryStage.clearHistory();
            primaryStage.setUser(null);
            new SetupLoginSelectionPage(databaseHelper, questionManager).show(primaryStage);
        });

        layout.getChildren().addAll(welcomeLabel, radioBox, continueButton, logoutButton);
        Scene welcomeScene = new Scene(layout, 800, 400);
        welcomeScene.getProperties().put("isWelcomePage", true); // allows checking of page properties
        primaryStage.setScene(welcomeScene);
        primaryStage.setTitle("Welcome Page");
    }

}
