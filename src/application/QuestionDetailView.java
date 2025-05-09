package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class QuestionDetailView {
    
    private Scene scene;
    private Question question;
    private QuestionManager questionManager;
    private DatabaseHelper databaseHelper;
    private User currentUser;
    private Runnable onBack; // callback to refresh discussion view when going back
    
    private VBox answersContainer;    
    private VBox editQuestionContainer;
    private VBox questionReplyContainer;  
        
    private Label fullTextLabel;
    private Label errorLabel;
    
    public QuestionDetailView(Question question, QuestionManager questionManager, DatabaseHelper databaseHelper, User currentUser, Runnable onBack) {
        this.question = question;
        this.questionManager = questionManager;
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
        this.onBack = onBack;
    }
    
    // overloaded constructor if no callback is needed.
    public QuestionDetailView(Question question, QuestionManager questionManager, DatabaseHelper databaseHelper, User currentUser) {
        this(question, questionManager, databaseHelper, currentUser, null);
    }
    
    public void show(CustomTrackedStage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
       
        VBox header = new VBox(10);
        header.setPadding(new Insets(10));
        
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Button backButton = BackButton.createBackButton(primaryStage);
        backButton.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            }
            primaryStage.goBack();
        });
        topRow.getChildren().add(backButton);
        
        Label titleLabel = new Label(question.getTitle());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label flagIcon = new Label("\u2691");  // Unicode flag symbol
        flagIcon.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
        if (hasPrivilege(currentUser) && databaseHelper.isQuestionFlagged(question.getId())) {
            titleLabel.setText(question.getTitle() + " ");
            HBox titleContainer = new HBox(5, titleLabel, flagIcon);
            titleContainer.setAlignment(Pos.CENTER_LEFT);
            titleLabel = new Label();
            titleLabel.setGraphic(titleContainer);
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button optionsButton = new Button("\u22EE"); 
        optionsButton.setStyle(
            "-fx-background-color: #e0e0e0; -fx-border-color: #ccc; " +
            "-fx-border-radius: 5px; -fx-background-radius: 5px; " +
            "-fx-font-size: 16px; -fx-padding: 5px;"
        );
        
        ContextMenu contextMenu = new ContextMenu();
        if (currentUser.getUserName().equals(question.getAuthor()) || 
            hasPrivilege(currentUser)) {
            
            MenuItem editQuestion = new MenuItem("Edit Question");
            editQuestion.setOnAction(e -> {
                if (currentUser.getUserName().equals(question.getAuthor())) {
                    if (editQuestionContainer.getChildren().isEmpty()) {
                        EditQuestionBox editBox = new EditQuestionBox(text -> {
                            try {
                                questionManager.editQuestiontext(question, text);
                                refreshContent();
                            } catch(Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                        editQuestionContainer.getChildren().add(editBox);
                    } else {
                        editQuestionContainer.getChildren().clear();
                    }
                } else {
                    errorLabel.setText("You aren't the user who posted this question.");
                }
            });

            MenuItem deleteItem = new MenuItem("Delete Question");
            deleteItem.setOnAction(e -> {
                boolean success = questionManager.deleteQuestion(question.getId());
                if (success) {
                    if (onBack != null) {
                        onBack.run(); // refresh discussion view
                    }
                    primaryStage.goBack(); // navigate back to discussion page
                } else {
                    errorLabel.setText("Error deleting question.");
                }
            });
            contextMenu.getItems().addAll(editQuestion, deleteItem);
        }
        
        // ensure requester has permission
        if (hasPrivilege(currentUser)) {
            MenuItem flagItem;
            if (databaseHelper.isQuestionFlagged(question.getId())) {
                flagItem = new MenuItem("Unflag Question");
            } else {
                flagItem = new MenuItem("Flag Question");
            }
            flagItem.setOnAction(e -> {
                boolean success;
                if (databaseHelper.isQuestionFlagged(question.getId())) {
                    success = databaseHelper.unflagQuestion(question.getId());
                } else {
                    success = databaseHelper.flagQuestion(question.getId(), currentUser.getName());
                }
                if (success) {
                    refreshContent();
                } else {
                    errorLabel.setText("Error updating flag status.");
                }
            });
            contextMenu.getItems().add(flagItem);
        }
        
        optionsButton.setOnAction(e -> {
            contextMenu.show(optionsButton, Side.BOTTOM, 0, 0);
        });
        
        HBox secondRow = new HBox(10);
        secondRow.setAlignment(Pos.CENTER_LEFT);
        secondRow.getChildren().addAll(titleLabel, spacer, optionsButton);
        header.getChildren().addAll(topRow, secondRow);
        
        fullTextLabel = new Label(question.getQuestionText());
        fullTextLabel.setWrapText(true);
        fullTextLabel.setStyle("-fx-font-size: 14px;");
        
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        editQuestionContainer = new VBox(10);
        questionReplyContainer = new VBox(10);
        
        Button replyButton = new Button("Reply");
        replyButton.setOnAction(e -> {
            if (questionReplyContainer.getChildren().isEmpty()) {
                ReplyBox replyBox = new ReplyBox(text -> {
                    try {
                        questionManager.createAnswer(currentUser.getUserName(), text, question);
                        refreshContent();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                });
                questionReplyContainer.getChildren().add(replyBox);
            } else {
                questionReplyContainer.getChildren().clear();
            }
        });
        
        VBox questionContainer = new VBox(10);
        questionContainer.getChildren().addAll(
            fullTextLabel, 
            errorLabel, 
            replyButton, 
            editQuestionContainer, 
            questionReplyContainer
        );
        
        answersContainer = new VBox(10);
        loadAnswers();
        ScrollPane answersScroll = new ScrollPane(answersContainer);
        answersScroll.setFitToWidth(true);
        
        VBox mainContent = new VBox(20);
        mainContent.getChildren().addAll(questionContainer, new Label("Answers:"), answersScroll);
        
        root.setTop(header);
        root.setCenter(mainContent);
        
        scene = new Scene(root, 900, 700);
        primaryStage.showScene(scene);
    }
    
    // helper method
    private boolean hasPrivilege(User currentUser2) {
		if ((currentUser2.getRoles().contains("Admin") 
				|| currentUser2.getRoles().contains("Instructor") 
				|| currentUser2.getRoles().contains("Staff") 
				|| currentUser2.getRoles().contains("Reviewer"))) return true;
		return false;
	}

    // load list of questions
    private void loadAnswers() {
        answersContainer.getChildren().clear();
        List<Answer> answerList = question.getAnswers();
        if (answerList == null) {
            return;
        }
        for (Answer a : answerList) {
            if (a == null) continue;
            if (a.getParentAnswerId() == null) {
                User answerAuthor = null;
                try {
                    answerAuthor = databaseHelper.getUser(a.getAuthor());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                String highestRole = "Student";
                if (answerAuthor != null && answerAuthor.getRoles() != null) {
                    highestRole = getHighestRole(answerAuthor.getRoles());
                }
                String displayName = highestRole.equalsIgnoreCase("Student")
                                     ? a.getAuthor()
                                     : highestRole + " " + a.getAuthor();
                Label authorLabel = new Label("Posted by: " + displayName);
                authorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                
                AnswerView aView = new AnswerView(a, question, questionManager, currentUser, question.getAnswers(), this::refreshContent);
                VBox answerBox = new VBox(5, authorLabel, aView);
                answerBox.setPadding(new Insets(5));
                answersContainer.getChildren().add(answerBox);
            }
        }
    }
    
    // helper method
    private String getHighestRole(String roles) {
        String[] roleOrder = {"Admin", "Instructor", "Staff", "Reviewer", "Student"};
        String[] userRoles = roles.split(",\\s*");
        for (String r : roleOrder) {
            for (String ur : userRoles) {
                if (ur.equalsIgnoreCase(r)) {
                    return r;
                }
            }
        }
        return "Student"; 
    }
    
    private void refreshContent() {
        questionManager.refreshQuestion(question);
        fullTextLabel.setText(question.getQuestionText());
        editQuestionContainer.getChildren().clear();
        questionReplyContainer.getChildren().clear();
        loadAnswers();
    }
}
