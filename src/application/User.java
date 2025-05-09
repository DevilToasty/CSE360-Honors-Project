package application;

import java.util.HashMap;
import java.util.Map;

import databasePart1.DatabaseHelper;

public class User {
    private String userName;
    private String password;
    private String email;
    private String roles; // e.g., "Admin, Student, Instructor, Staff"
    private String name;
    private Map<String, Double> approvedReviewers; // maps reviewer username to rating (0.0 to 5.0)

    // constructor with minimal fields
    public User(String userName, String password, String roles) {
        this.userName = userName;
        this.password = password;
        this.roles = roles;
        this.email = null;
        this.name = "";
        this.approvedReviewers = new HashMap<>();
    }
    
    // constructor with email
    public User(String userName, String password, String email, String roles) {
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.roles = roles;
        this.name = "";
        this.approvedReviewers = new HashMap<>();
    }
    
    // constructor with name, email, and roles
    public User(String userName, String password, String name, String email, String roles) {
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.name = name;
        this.roles = roles;
        this.approvedReviewers = new HashMap<>();
    }
    
    // constructor with approvedReviewers map
    public User(String userName, String password, String name, String email, String roles, Map<String, Double> approvedReviewers) {
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.name = name;
        this.roles = roles;
        // make new map to prevent memory changes from different classes
        this.approvedReviewers = new HashMap<>(approvedReviewers); 
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getRoles() {
        return roles;
    }
    
    public Map<String, Double> getApprovedReviewers() {
        return approvedReviewers;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setRoles(String roles) {
        this.roles = roles;
    }
    
    public void setApprovedReviewers(Map<String, Double> approvedReviewers) {
        this.approvedReviewers = new HashMap<>(approvedReviewers);
    }
    
    public void addApprovedReviewer(String reviewer, double rating, DatabaseHelper dbHelper) {
        if (reviewer == null || reviewer.trim().isEmpty()) return;
        reviewer = reviewer.trim();
        approvedReviewers.put(reviewer, rating);
        dbHelper.addApprovedReviewer(this.userName, reviewer, rating);
    }
    
    public void updateReviewerRating(String reviewer, double rating, DatabaseHelper dbHelper) {
        if (reviewer == null || reviewer.trim().isEmpty()) return;
        reviewer = reviewer.trim();
        if (approvedReviewers.containsKey(reviewer)) {
            approvedReviewers.put(reviewer, rating);
            dbHelper.updateApprovedReviewerRating(this.userName, reviewer, rating);
        }
    }
    
    public void removeApprovedReviewer(String reviewer, DatabaseHelper dbHelper) {
        if (reviewer == null || reviewer.trim().isEmpty()) return;
        reviewer = reviewer.trim();
        approvedReviewers.remove(reviewer);
        dbHelper.removeApprovedReviewer(this.userName, reviewer);
    }
    
    public boolean isReviewerApproved(String reviewer) {
        if (reviewer == null || reviewer.trim().isEmpty()) return false;
        reviewer = reviewer.trim();
        return approvedReviewers.containsKey(reviewer);
    }
    
    public Double getReviewerRating(String reviewer) {
        if (reviewer == null || reviewer.trim().isEmpty()) return null;
        reviewer = reviewer.trim();
        return approvedReviewers.get(reviewer);
    }
}
