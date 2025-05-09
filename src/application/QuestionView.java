package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class QuestionView extends VBox {
    private Question question;
    private DatabaseHelper databaseHelper;
    private User currentUser;
    
    public QuestionView(Question question, DatabaseHelper databaseHelper, User currentUser) {
        this.question = question;
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
        setPadding(new Insets(10));
        setSpacing(8);
        setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        buildView();
    }
    
    private void buildView() {

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(question.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        header.getChildren().add(titleLabel);
        
        // ensure user has permissions
        if (currentUser != null && hasPrivilege(currentUser) 
                && databaseHelper.isQuestionFlagged(question.getId())) {
            Label flagMarker = new Label("\u2691"); // Unicode flag (you can change this to an icon image if you wish)
            flagMarker.setStyle("-fx-text-fill: red; -fx-font-size: 16px; -fx-padding: 0 5 0 5;");
            header.getChildren().add(flagMarker);
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);
        
        HBox authorBox = new HBox(5);
        authorBox.setAlignment(Pos.CENTER);
        if (question.hasApprovedAnswer()) {
            Label checkmarkLabel = new Label("\u2714"); // Unicode checkmark
            checkmarkLabel.setStyle(
                "-fx-background-color: green; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 10px; " +
                "-fx-min-width: 16px; " +
                "-fx-min-height: 16px; " +
                "-fx-alignment: center; " +
                "-fx-background-radius: 3px;"
            );
            authorBox.getChildren().add(checkmarkLabel);
        }
        Label authorLabel = new Label(question.getAuthor());
        authorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        authorBox.getChildren().add(authorLabel);
        header.getChildren().add(authorBox);
        
        String snippet = getSnippet(question.getQuestionText(), 40);
        Label snippetLabel = new Label(snippet);
        snippetLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
        snippetLabel.setWrapText(true);
        
        Label answerCountLabel = new Label("Answers: " + question.getAnswers().size());
        answerCountLabel.setStyle("-fx-font-size: 12px;");
        
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().add(answerCountLabel);
        
        getChildren().addAll(header, snippetLabel, footer);
    }
    
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
    
    private boolean hasPrivilege(User user) {
        String roles = user.getRoles().toLowerCase();
        return roles.contains("Admin") || roles.contains("Instructor") || roles.contains("Staff") || roles.contains("Reviewer");
    }
    
    public Question getQuestion() {
        return question;
    }
}
