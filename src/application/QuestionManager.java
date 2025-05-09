package application;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import databasePart1.DatabaseHelper;

public class QuestionManager {
    private List<Question> questions;
    private List<Answer> answers;
    private final DatabaseHelper databaseHelper;
    
    public QuestionManager(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
        this.questions = databaseHelper.getAllQuestionsFromDB();
        this.answers = new ArrayList<>();
        for (Question q : questions) {
            List<Answer> qAnswers = databaseHelper.getAnswersForQuestion(q.getId());
            for (Answer a : qAnswers) {
                q.addAnswer(a);
                answers.add(a);
            }
        }
    }
    
    public void createQuestion(String author, String questionTitle, String questionText) {
        if (!isValidQuestionText(questionText)) {
            throw new IllegalArgumentException("Question must be between 10 and 300 words.");
        }
        Question q = new Question(author, questionTitle, questionText);
        questions.add(q);
        databaseHelper.insertQuestion(q);
    }
    
    public void createQuestion(String author, String questionTitle, String questionText, Question referencedQuestion) {
        if (!isValidQuestionText(questionText)) {
            throw new IllegalArgumentException("Question must be between 10 and 300 words.");
        }
        Question q = new Question(author, questionTitle, questionText, referencedQuestion);
        questions.add(q);
        databaseHelper.insertQuestion(q);
    }
    
    public void createAnswer(String author, String answerText, Question question) {
        if (!isValidAnswerText(answerText)) {
            throw new IllegalArgumentException("Answer must be between 1 and 500 words.");
        }
        Answer a = new Answer(answerText, author);
        question.addAnswer(a);
        answers.add(a);
        databaseHelper.insertAnswer(a, question.getId());
    }
    
    public void editQuestiontext(Question question, String newText) {
    	question.updateQuestionText(newText);
    	databaseHelper.updateQuestion(question);
    }
    
    public void markAnswerAsSolution(Question question, Answer answer) {
        question.markAnswerAsSolution(answer);
        databaseHelper.updateAnswer(answer);
        databaseHelper.updateQuestion(question);
    }
    
    public void unmarkAnswerAsSolution(Question question, Answer answer) {
        question.unmarkAnswerAsSolution(answer);
        databaseHelper.updateAnswer(answer);
        databaseHelper.updateQuestion(question);
    }
    
    public boolean deleteQuestion(UUID questionId) {
        Question q = findQuestionById(questionId);
        if (q != null) {
            List<Answer> answersToDelete = new ArrayList<>(q.getAnswers());
            for (Answer a : answersToDelete) {
                // delete the answer from the database.
                boolean answerDeleted = databaseHelper.deleteAnswer(a.getId());
                if (!answerDeleted) {
                    // if any answer fails to delete, return false.
                    System.err.println("Error deleting answer with ID: " + a.getId());
                    return false;
                }
                // remove the answer from the question and the global list
                q.removeAnswer(a);
                answers.remove(a);
            }
            
            questions.remove(q);
            
            boolean questionDeleted = databaseHelper.deleteQuestion(questionId);
            if (!questionDeleted) {
                System.err.println("Error deleting question with ID: " + questionId);
            }
            return questionDeleted;
        }
        return false;
    }

    
    public boolean deleteAnswer(UUID answerId) {
        for (Question q : questions) {
            List<Answer> answersCopy = new ArrayList<>(q.getAnswers());
            for (Answer a : answersCopy) {
                if (a.getId().equals(answerId)) {
                    q.removeAnswer(a);
                    answers.remove(a);
                    boolean dbDeleted = databaseHelper.deleteAnswer(answerId);
                    if (!dbDeleted) {
                        System.err.println("Error deleting answer from DB.");
                        return false;
                    }
                    databaseHelper.updateQuestion(q);
                    return true;
                }
            }
        }
        return false;
    }
    
    // search method filters by keywords
    public List<Question> searchQuestionsByKeyword(String keyword) {
        List<Question> matching = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Keyword cannot be empty.");
        }
        for (Question q : questions) {
            if (q.getQuestionText().toLowerCase().contains(keyword.toLowerCase()) ||
                q.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                matching.add(q);
            }
        }
        return matching;
    }
    
    public List<Answer> searchAnswers(String keyword) {
        List<Answer> matching = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Keyword cannot be empty.");
        }
        for (Question q : questions) {
            for (Answer a : q.getAnswers()) {
                if (a.getAnswerText().toLowerCase().contains(keyword.toLowerCase())) {
                    matching.add(a);
                }
            }
        }
        return matching;
    }
    
    // returns all answers
    public List<Answer> getAnswers() {
        List<Answer> aList = new ArrayList<>();
        for (Question q : questions) {
            for (Answer a : q.getAnswers()) {
                    aList.add(a);
            }
        }
        return aList;
    }
    
    public List<Answer> getAllApprovedSolutions() {
        List<Answer> approved = new ArrayList<>();
        for (Question q : questions) {
            approved.addAll(q.getApprovedSolutions());
        }
        return approved;
    }
    
    public List<Question> getAllQuestions() {
        return new ArrayList<>(questions);
    }
    
    public List<Question> getAnsweredQuestions() {
        List<Question> answered = new ArrayList<>();
        for (Question q : questions) {
            if (q.isResolved()) {
                answered.add(q);
            }
        }
        return answered;
    }
    
    public List<Question> getUnansweredQuestions() {
        List<Question> unanswered = new ArrayList<>();
        for (Question q : questions) {
            if (!q.isResolved()) {
                unanswered.add(q);
            }
        }
        return unanswered;
    }
    
    public List<User> getReviewers() throws SQLException {
        List<User> allUsers = databaseHelper.getUsers();
        List<User> reviewers = new ArrayList<>();
        for (User u : allUsers) {
            if (u.getRoles() != null && u.getRoles().toLowerCase().contains("reviewer")) {
                reviewers.add(u);
            }
        }
        return reviewers;
    }
    
    public Question findQuestionById(UUID id) {
        for (Question q : questions) {
            if (q.getId().equals(id)) {
                return q;
            }
        }
        return null;
    }
    
    public Answer findAnswerById(UUID answerId) {
        for (Question q : questions) {
            for (Answer a : q.getAnswers()) {
                if (a.getId().equals(answerId)) {
                    return a;
                }
            }
        }
        return null;
    }
    
    public List<Question> findQuestionsByAuthor(String username) {
        List<Question> userQuestions = new ArrayList<>();
        for (Question q : questions) {
            if (q.getAuthor().equals(username)) {
                userQuestions.add(q);
            }
        }
        return userQuestions;
    }
    
    public List<Answer> findAnswersByAuthor(String username) {
        List<Answer> userAnswers = new ArrayList<>();
        for (Question q : questions) {
            for (Answer a : q.getAnswers()) {
                if (a.getAuthor().equals(username)) {
                    userAnswers.add(a);
                }
            }
        }
        return userAnswers;
    }
    
    // updates question object from db
    public void refreshQuestion(Question question) {
        List<Answer> freshAnswers = databaseHelper.getAnswersForQuestion(question.getId());
        question.refreshAnswers(freshAnswers);
    }
    
    // method to update question object and database
    public void updateQuestionText(Question question, String newText) {
        question.updateQuestionText(newText);
        databaseHelper.updateQuestion(question);
    }
    
    public void updateAnswerText(Answer answer, String newText) {
        answer.updateAnswerText(newText);
        databaseHelper.updateAnswer(answer); 
    }
    
    private boolean isValidQuestionText(String text) {
    	if (text == null) return false;
        int wordCount = text.trim().split("\\s+").length;
        return wordCount >= 10 && wordCount <= 300;
    }
    
    private boolean isValidAnswerText(String text) {
        int wordCount = text.trim().split("\\s+").length;
        return wordCount >= 1 && wordCount <= 500;
    }
}
