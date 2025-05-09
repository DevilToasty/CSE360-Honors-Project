package application;

import javafx.stage.Stage;
import javafx.scene.Scene;
import java.util.Stack;

import databasePart1.DatabaseHelper;

public class CustomTrackedStage extends Stage {

    private Stack<Scene> sceneHistory = new Stack<>();
    private User user;
    private final DatabaseHelper databaseHelper;
    private final User currentUser;
    private final QuestionManager questionManager;

    public CustomTrackedStage(DatabaseHelper databaseHelper, QuestionManager questionManager, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
        this.questionManager = questionManager;
    }

    public void showScene(Scene newScene) {
        if (this.getScene() != null) {
            sceneHistory.push(this.getScene());
        }
        this.setScene(newScene);
        this.show();
    }

    // custom method to go to last used page (using stack)
    public void goBack() {
        if (!sceneHistory.isEmpty()) {
            Scene previousScene = sceneHistory.pop();
            Boolean isWelcome = (Boolean) previousScene.getProperties().get("isWelcomePage");
            if (isWelcome != null && isWelcome) { // to check if we need to update login stuff
                new WelcomeLoginPage(databaseHelper, questionManager).show(this, this.getUser());
                return;
            }
            this.setScene(previousScene);
            this.show();
        } else {
            System.out.println("No previous scene.");
        }
    }

    public void setLastScene(Scene newScene) {
        if (newScene != null) {
            sceneHistory.push(newScene);
        }
    }

    public void clearHistory() {
        sceneHistory.clear();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
