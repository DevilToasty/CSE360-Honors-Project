package application;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class EditAnswerBox extends VBox {
    private TextArea textArea;
    private Label wordCountLabel;
    private Button cancelButton;
    private Button sendButton;
    
    // callback interface so the parent can process reply
    public interface EditCallback {
        void onSend(String text);
    }
    
    public EditAnswerBox(EditCallback callback) {
        setSpacing(5);
        setPadding(new Insets(5));
        
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefHeight(100);
        
        wordCountLabel = new Label("Word count: 0");
        
        // updates whenever new words are typed so we can dynamically update the counter
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
            int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length; // grammar formatting
            if (wordCount > 500 || wordCount < 1) {
                wordCountLabel.setText("Word count must be less than 500 words.");
            } else {
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
