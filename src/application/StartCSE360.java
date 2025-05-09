package application;

import javafx.application.Application;
import javafx.stage.Stage;
import java.sql.SQLException;
import databasePart1.DatabaseHelper;

public class StartCSE360 extends Application {
    private static final DatabaseHelper databaseHelper = new DatabaseHelper();
    private final QuestionManager questionManager;
    
    public StartCSE360() {
        try {
            databaseHelper.connectToDatabase();  // connect to the database before creating the manager
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        questionManager = new QuestionManager(databaseHelper);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        CustomTrackedStage trackedStage = new CustomTrackedStage(databaseHelper, questionManager, null);
        try {
			if (databaseHelper.isDatabaseEmpty()) {
			    new FirstPage(databaseHelper, questionManager).show(trackedStage);
			} else {
			    new SetupLoginSelectionPage(databaseHelper, questionManager).show(trackedStage);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
}
