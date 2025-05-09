package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.List;

public class AnswerView extends VBox {
    private Answer answer;
    private Question question;
    private QuestionManager questionManager;
    private User currentUser;
    private List<Answer> allAnswers; // list of all answers for the question

    private Label answerLabel;
    private Button seeMoreButton;
    private boolean expanded = false;
    private VBox replyContainer;
    private VBox editContainer;
    private VBox nestedRepliesContainer;
    private Label errorLabel; // for showing errors in editing

    private Runnable refreshCallback; // callback to refresh the full view

    // public constructor
    public AnswerView(Answer answer, Question question, QuestionManager questionManager, User currentUser, List<Answer> allAnswers, Runnable refreshCallback) {
        this.answer = answer;
        this.question = question;
        this.questionManager = questionManager;
        this.currentUser = currentUser;
        this.allAnswers = allAnswers;
        this.refreshCallback = refreshCallback;
        setPadding(new Insets(10));
        setSpacing(8);
        setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        buildView();
    }

    private void buildView() {

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // author box with checkmark
        HBox authorBox = new HBox(5);
        authorBox.setAlignment(Pos.CENTER);
        if (question.hasApprovedAnswer() && (answer.getParentAnswerId() == null)) {
            for (Answer approvedAnswer : question.getApprovedSolutions()) {
                if (approvedAnswer.getId().equals(answer.getId())) {
                    Label checkmarkLabel = new Label("\u2714"); // Unicode checkmark
                    checkmarkLabel.setStyle(
                        "-fx-background-color: green; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-min-width: 16px; " +
                        "-fx-min-height: 16px; " +
                        "-fx-alignment: center; " +
                        "-fx-background-radius: 3px;"
                    );
                    authorBox.getChildren().add(checkmarkLabel);
                    break;
                }
            }
        }
        Label authorLabel = new Label("By: " + answer.getAuthor());
        authorLabel.setStyle("-fx-font-weight: bold;");
        authorBox.getChildren().add(authorLabel);
        header.getChildren().add(authorBox);

        // if the current user is the question author, add an options menu.
      
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        Button optionsButton = new Button("\u22EE");  // unicode ellipsis
        optionsButton.setStyle("-fx-background-color: transparent; -fx-font-size: 14px;");

        ContextMenu contextMenu = new ContextMenu();
        
        // check permissions
        if (currentUser.getUserName().equals(question.getAuthor())) {
	        MenuItem toggleApprove = new MenuItem(answer.isApprovedSolution() ? "Unapprove" : "Approve");
	        toggleApprove.setOnAction(e -> {
	            if (answer.isApprovedSolution()) {
	                questionManager.unmarkAnswerAsSolution(question, answer);
	            } else {
	                questionManager.markAnswerAsSolution(question, answer);
	            }
	            questionManager.refreshQuestion(question);
	            if (refreshCallback != null) {
	                refreshCallback.run();
	            }
	        });
	        contextMenu.getItems().add(toggleApprove);
	        
	        MenuItem editAnswer = new MenuItem("Edit Answer");
	        editAnswer.setOnAction(e -> {
	        	if (currentUser.getUserName().equals(answer.getAuthor())) {
	                toggleEditBox();
	            } else {
	                errorLabel.setText("You aren't the user who posted this answer.");
	            }
	        });
	        contextMenu.getItems().add(editAnswer);
	        
	        MenuItem deleteItem = new MenuItem("Delete Answer");
	        deleteItem.setOnAction(e -> {
	            boolean success = questionManager.deleteAnswer(answer.getId());
	            if (success && refreshCallback != null) {
	                refreshCallback.run();
	            }
	        });
	        contextMenu.getItems().add(deleteItem);
        }
        
        MenuItem reportItem = new MenuItem("Report");
        reportItem.setOnAction(e -> {
            // to do
        });
        
        contextMenu.getItems().add(reportItem);
        
        optionsButton.setOnAction(e -> {
            contextMenu.show(optionsButton, Side.BOTTOM, 0, 0);
        });
        header.getChildren().add(optionsButton);

        answerLabel = new Label(getSnippet(answer.getAnswerText(), 40));
        answerLabel.setWrapText(true);

        seeMoreButton = new Button("See more");
        if (answer.getAnswerText().split("\\s+").length <= 40) {
            seeMoreButton.setVisible(false);
        }
        seeMoreButton.setOnAction(e -> toggleExpanded());

        Button replyButton = new Button("Reply");
        replyButton.setOnAction(e -> toggleReplyBox());
        replyContainer = new VBox();
        replyContainer.setSpacing(5);

        editContainer = new VBox();
        editContainer.setSpacing(5);
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        nestedRepliesContainer = new VBox();
        nestedRepliesContainer.setSpacing(5);
        nestedRepliesContainer.setPadding(new Insets(10, 0, 0, 20));

        // add all GUI elements
        getChildren().addAll(
        		header, 
        		answerLabel, 
        		seeMoreButton, 
        		replyButton, 
        		replyContainer, 
        		errorLabel, 
        		editContainer, 
        		nestedRepliesContainer);

        // call method to load text
        loadNestedReplies();
    }

    // returns a snippet
    private String getSnippet(String fullText, int maxWords) {
        if (fullText == null || fullText.isEmpty()) return "";
        String[] words = fullText.split("\\s+");
        int count = Math.min(words.length, maxWords);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(words[i]).append(" ");
        }
        if (words.length > maxWords) {
            sb.append("...");
        }
        return sb.toString().trim();
    }

    // toggle between expanded and collapsed view
    private void toggleExpanded() {
        if (expanded) {
            answerLabel.setText(getSnippet(answer.getAnswerText(), 40));
            seeMoreButton.setText("See more");
            expanded = false;
        } else {
            answerLabel.setText(answer.getAnswerText());
            seeMoreButton.setText("See less");
            expanded = true;
        }
    }

    // toggles the reply box
    private void toggleReplyBox() {
        if (replyContainer.getChildren().isEmpty()) {
            ReplyBox replyBox = new ReplyBox(text -> {
                try {
                    // create a new answer as a reply (nested reply) to this answer
                    Answer newAnswer = new Answer(text, currentUser.getUserName(), answer.getId());
                    question.getAnswers().add(newAnswer);
                    questionManager.createAnswer(currentUser.getUserName(), text, question);
                    replyContainer.getChildren().clear();
                    nestedRepliesContainer.getChildren().clear();
                    loadNestedReplies();
                    if (refreshCallback != null) {
                        refreshCallback.run();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            replyContainer.getChildren().add(replyBox);
        } else {
            replyContainer.getChildren().clear();
        }
    }

    // toggles the edit box for modifying this answer
    private void toggleEditBox() {
        if (editContainer.getChildren().isEmpty()) {
            EditAnswerBox editBox = new EditAnswerBox(text -> {
                try {
                    // update the answer text
                    questionManager.updateAnswerText(answer, text);

                    // update the UI
                    if (!expanded) {
                        answerLabel.setText(getSnippet(answer.getAnswerText(), 40));
                    } else {
                        answerLabel.setText(answer.getAnswerText());
                    }
                    
                    editContainer.getChildren().clear();
                    nestedRepliesContainer.getChildren().clear();
                    loadNestedReplies();
                    if (refreshCallback != null) {
                        refreshCallback.run();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            editContainer.getChildren().add(editBox);
        } else {
            editContainer.getChildren().clear();
        }
    }

    // loads nested replies for this answer (broken rn)
    private void loadNestedReplies() {
        nestedRepliesContainer.getChildren().clear();
        for (Answer a : allAnswers) {
            if (a.getParentAnswerId() != null && a.getParentAnswerId().equals(answer.getId())) {
                AnswerView nestedView = new AnswerView(a, question, questionManager, currentUser, allAnswers, refreshCallback);
                nestedRepliesContainer.getChildren().add(nestedView);
            }
        }
    }
}
