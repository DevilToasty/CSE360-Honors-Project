package application;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class EditQuestionBox extends VBox {
    private TextArea textArea;
    private Label wordCountLabel;
    private Button cancelButton;
    private Button sendButton;
    
    // callback interface so the parent can process reply
    public interface EditCallback {
        void onSend(String text);
    }
    
    public EditQuestionBox(EditCallback callback) {
        setSpacing(5);
        setPadding(new Insets(5));
        
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefHeight(100);
        
        wordCountLabel = new Label("Word count: 0");
        
        // listener to update dynamically
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            int wordCount = newText.trim().isEmpty() ? 0 : newText.trim().split("\\s+").length;
            wordCountLabel.setText("Word count: " + wordCount);
        });
        
        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            textArea.clear();
            if(getParent() instanceof VBox) {
                ((VBox)getParent()).getChildren().remove(this);
            }
        });
        
        sendButton = new Button("Save");
        sendButton.setOnAction(e -> {
            String text = textArea.getText().trim();
            int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;
            if (wordCount < 10) {
            	wordCountLabel.setText("Question must be more than 10 words. Current word count: " + wordCount);
                return;
            } else if (wordCount > 300) {
            	wordCountLabel.setText("Question must not exceed 300 words. Current word count: " + wordCount);
                return;} else {
                callback.onSend(text);
                if(getParent() instanceof VBox) {
                    ((VBox)getParent()).getChildren().remove(this);
                }
            }
        });
        
        HBox buttons = new HBox(10, cancelButton, sendButton);
        getChildren().addAll(textArea, wordCountLabel, buttons);
    }
}
