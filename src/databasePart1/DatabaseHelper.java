package databasePart1;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import application.Answer;
import application.PrivateFeedback;
import application.PrivateMessage;
import application.Question;
import application.Review;
import application.ReviewerRequest;
import application.User;

public class DatabaseHelper {
    static final String JDBC_DRIVER = "org.h2.Driver";
    static final String DB_URL = "jdbc:h2:~/FoundationDatabase";
    static final String USER = "sa";
    static final String PASS = "";
    private Connection connection = null;
    private Statement statement = null;
    
    private String sanitize(String input) {
        if (input == null) return null;
        return input.trim();
    }
    
    public void connectToDatabase() throws SQLException {
		try { 
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			System.out.println("Connecting to database...");
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connection.createStatement(); 
			// You can use this command to clear the database and restart from fresh. DO NOT DELETE
			//statement.execute("DROP ALL OBJECTS");

			createTables();  // Create the necessary tables if they don't exist
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}
    
    private void createTables() throws SQLException {
        String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "userName VARCHAR(255) UNIQUE, "
                + "password VARCHAR(255), "
                + "email VARCHAR(255), "
                + "name VARCHAR(255), "
                + "roles VARCHAR(255), "
                + "CONSTRAINT unique_email UNIQUE (email))";
        statement.execute(userTable);

        // table for list of reviewers on each student 
        String approvedReviewersTable = "CREATE TABLE IF NOT EXISTS ApprovedReviewers ("
                + "ownerUserName VARCHAR(255) NOT NULL, "
                + "reviewerName VARCHAR(255) NOT NULL, "
                + "reviewerRating DOUBLE, "
                + "PRIMARY KEY (ownerUserName, reviewerName))";
        statement.execute(approvedReviewersTable);

        // invite codes
        String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
                + "code VARCHAR(10) PRIMARY KEY, "
                + "isUsed BOOLEAN DEFAULT FALSE)";
        statement.execute(invitationCodesTable);

        // OTP setting 
        String userOTPAccess = "CREATE TABLE IF NOT EXISTS UserOTP ("
                + "userName VARCHAR(255) UNIQUE, "
                + "tempPassword VARCHAR(255), "
                + "isUsed BOOLEAN DEFAULT FALSE)";
        statement.execute(userOTPAccess);

        // list of questions to be converted to question objects
        String questionsTable = "CREATE TABLE IF NOT EXISTS Questions ("
                + "id UUID PRIMARY KEY, "
                + "author VARCHAR(255) NOT NULL, "
                + "questionTitle CLOB NOT NULL, "
                + "questionText CLOB NOT NULL, "
                + "referencedQuestionId UUID, "
                + "timestamp TIMESTAMP NOT NULL, "
                + "resolved BOOLEAN DEFAULT FALSE)";
        statement.execute(questionsTable);

        // list of answers
        String answersTable = "CREATE TABLE IF NOT EXISTS Answers ("
                + "id UUID PRIMARY KEY, "
                + "questionId UUID NOT NULL, "
                + "answerText CLOB NOT NULL, "
                + "author VARCHAR(255) NOT NULL, "
                + "isApprovedSolution BOOLEAN DEFAULT FALSE, "
                + "timestamp TIMESTAMP NOT NULL, "
                + "parentAnswerId UUID, "
                + "FOREIGN KEY (questionId) REFERENCES Questions(id), "
                + "FOREIGN KEY (parentAnswerId) REFERENCES Answers(id))";
        statement.execute(answersTable);

        // table to hold people who requested reviewer role
        String reviewerRequestTable = "CREATE TABLE IF NOT EXISTS ReviewerRequests ("
                + "id UUID PRIMARY KEY, "
                + "requestUser VARCHAR(255) NOT NULL, "
                + "isApproved BOOLEAN DEFAULT NULL)";
        statement.execute(reviewerRequestTable);

        // table to hold list of reviews on answers
        String reviewsTable = "CREATE TABLE IF NOT EXISTS Reviews ("
                + "id UUID PRIMARY KEY, "
                + "reviewText CLOB NOT NULL, "
                + "reviewer VARCHAR(255) NOT NULL, "
                + "answerId UUID NOT NULL, "
                + "timestamp TIMESTAMP NOT NULL, "
                + "privateFeedbackCount INT DEFAULT 0, "
                + "previousReviewId UUID)";
        statement.execute(reviewsTable);

        // private feedback from reviews
        String privateFeedbackTable = "CREATE TABLE IF NOT EXISTS PrivateFeedback ("
                + "id UUID PRIMARY KEY, "
                + "fromUser VARCHAR(255) NOT NULL, "
                + "reviewer VARCHAR(255) NOT NULL, "
                + "feedback CLOB NOT NULL, "
                + "timestamp TIMESTAMP NOT NULL)";
        statement.execute(privateFeedbackTable);

        // private messaging table
        String privateMessagesTable = "CREATE TABLE IF NOT EXISTS PrivateMessages ("
                + "id UUID PRIMARY KEY, "
                + "sender VARCHAR(255) NOT NULL, "
                + "recipient VARCHAR(255) NOT NULL, "
                + "subject VARCHAR(255) NOT NULL, "
                + "message CLOB NOT NULL, "
                + "timestamp TIMESTAMP NOT NULL)";
        statement.execute(privateMessagesTable);
        
        // stores question ID of flagged questions
        String flaggedQuestionsTable = "CREATE TABLE IF NOT EXISTS FlaggedQuestions ("
                + "questionId UUID PRIMARY KEY, "      // each flagged question appears only once
                + "flaggedBy VARCHAR(255) NOT NULL, "
                + "flagTimestamp TIMESTAMP NOT NULL)";
        statement.execute(flaggedQuestionsTable);

        // stores answer ID of flagged answers
        String flaggedAnswersTable = "CREATE TABLE IF NOT EXISTS FlaggedAnswers ("
                + "answerId UUID PRIMARY KEY, "
                + "flaggedBy VARCHAR(255) NOT NULL, "
                + "flagTimestamp TIMESTAMP NOT NULL)";
        statement.execute(flaggedAnswersTable);
    }

    public boolean isDatabaseEmpty() throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM cse360users";
        ResultSet resultSet = statement.executeQuery(query);
        if(resultSet.next()){
            return resultSet.getInt("count") == 0;
        }
        return true;
    }
    
    // user methods
    
    public void register(User user) throws SQLException {
        String insertUser = "INSERT INTO cse360users (userName, password, roles, email, name) VALUES (?, ?, ?, ?, ?)";
        try(PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
            pstmt.setString(1, sanitize(user.getUserName()));
            pstmt.setString(2, sanitize(user.getPassword()));
            pstmt.setString(3, sanitize(user.getRoles()));
            pstmt.setString(4, sanitize(user.getEmail()));
            pstmt.setString(5, sanitize(user.getName()));
            pstmt.executeUpdate();
        }
    }
    
    public boolean login(User user) throws SQLException {
        String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ? AND roles = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(user.getUserName()));
            pstmt.setString(2, sanitize(user.getPassword()));
            pstmt.setString(3, sanitize(user.getRoles()));
            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    this.markUserOTPAsUsed(sanitize(user.getUserName()));
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
    
    public List<User> getUsers() throws SQLException {
        List<User> userList = new ArrayList<>();
        String query = "SELECT * FROM cse360users";
        try(Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {
            while(rs.next()){
                String userName = sanitize(rs.getString("userName"));
                String password = sanitize(rs.getString("password"));
                String email = sanitize(rs.getString("email"));
                String name = sanitize(rs.getString("name"));
                String roles = sanitize(rs.getString("roles"));
                Map<String, Double> approvedReviewers = getApprovedReviewers(userName);
                User user = new User(userName, password, name, email, roles);
                user.setApprovedReviewers(approvedReviewers);
                userList.add(user);
            }
        }
        return userList;
    }
    
    public User getUser(String username) throws SQLException {
        String query = "SELECT * FROM cse360users WHERE userName = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(username));
            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    String userName = sanitize(rs.getString("userName"));
                    String password = sanitize(rs.getString("password"));
                    String email = sanitize(rs.getString("email"));
                    String name = sanitize(rs.getString("name"));
                    String roles = sanitize(rs.getString("roles"));
                    Map<String, Double> approvedReviewers = getApprovedReviewers(userName);
                    return new User(userName, password, name, email, roles, approvedReviewers);
                }
            }
        }
        return null;
    }
    
    // updates 
    public boolean updateApprovedReviewerRating(String ownerUserName, String reviewerName, double rating) {
        String query = "UPDATE ApprovedReviewers SET reviewerRating = ? WHERE ownerUserName = ? AND reviewerName = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setDouble(1, rating);
            pstmt.setString(2, sanitize(ownerUserName));
            pstmt.setString(3, sanitize(reviewerName));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean removeApprovedReviewer(String ownerUserName, String reviewerName) {
        String query = "DELETE FROM ApprovedReviewers WHERE ownerUserName = ? AND reviewerName = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(ownerUserName));
            pstmt.setString(2, sanitize(reviewerName));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // returns map of approved reviewers for a username
    public Map<String, Double> getApprovedReviewers(String ownerUserName) {
        Map<String, Double> map = new HashMap<>();
        String query = "SELECT reviewerName, reviewerRating FROM ApprovedReviewers WHERE ownerUserName = ?";
        try(PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(ownerUserName));
            try(ResultSet rs = pstmt.executeQuery()){
                while(rs.next()){
                    String reviewerName = sanitize(rs.getString("reviewerName"));
                    double rating = rs.getDouble("reviewerRating");
                    map.put(reviewerName, rating);
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
    
    // its kinda self explanitory now
    public boolean updateApprovedReviewers(String userName, String approvedReviewers) {
        String query = "UPDATE cse360users SET approvedReviewers = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(approvedReviewers));
            pstmt.setString(2, sanitize(userName));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // a scale of 0.0 -> 5.0 for each users dictionary 
    public boolean updateUserRating(String userName, Double rating) {
        String query = "UPDATE cse360users SET reviewerRating = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            if (rating != null) {
                pstmt.setDouble(1, rating);
            } else {
                pstmt.setNull(1, Types.DOUBLE);
            }
            pstmt.setString(2, sanitize(userName));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // sanatizes string in, and ensures the password passes 
    public boolean changePassword(String username, String newPassword) {
        String query = "SELECT * FROM cse360users WHERE userName = ?";
        String updatePasswordQuery = "UPDATE cse360users SET password = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(username));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement updateStmt = connection.prepareStatement(updatePasswordQuery)) {
                        updateStmt.setString(1, sanitize(newPassword));
                        updateStmt.setString(2, sanitize(username));
                        int rowsUpdated = updateStmt.executeUpdate();
                        if (rowsUpdated > 0) {
                            return true;
                        }
                    }
                } else {
                    System.out.println("User not found.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // checks if username is in user table
    public boolean doesUserExist(String userName) {
        String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(userName));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // deletes user from table
    public boolean deleteUser(String userName) {
        String query = "DELETE FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(userName));
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            } else {
                System.out.println("User " + sanitize(userName) + " not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String generateInvitationCode() {
        String code = UUID.randomUUID().toString().substring(0, 4);
        String query = "INSERT INTO InvitationCodes (code) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(code));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return code;
    }

    // checks if invite code exists and unused
    public boolean validateInvitationCode(String code) {
        String query = "SELECT * FROM InvitationCodes WHERE code = ? AND isUsed = FALSE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(code));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                markInvitationCodeAsUsed(sanitize(code));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // updates invite table with used code
    private void markInvitationCodeAsUsed(String code) {
        String query = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(code));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // super clean function to get user roles from string
    public boolean hasRole(String username, String role) {
        if (getUserRoles(username).contains(sanitize(role))) return true;
        return false;
    }

    // checks for role # to have validation for other features
    public int getRoleCount(String username) {
        ArrayList<String> roleList = new ArrayList<String>();
        roleList.add("Staff");
        roleList.add("Student");
        roleList.add("Instructor");
        roleList.add("Admin");
        roleList.add("Reviewer");
        int count = 0;
        for (String r : roleList) {
            if (getUserRoles(username).contains(sanitize(r))) count++;
        }
        return count;
    }

    // helper to make sure admin it above 0
    public int getAdminCount() {
        String query = "SELECT COUNT(*) AS count FROM cse360users WHERE roles LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, "%Admin%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // returns string in format "Student, Admin, Teacher" etc.
    public String getUserRoles(String userName) {
        String query = "SELECT roles FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(userName));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return sanitize(rs.getString("roles"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // has to check if the user already has a role to make sure that you don't duplicate
    public void addUserRole(String username, String role) {
        String query = "SELECT roles FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(username));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String rolesString = sanitize(rs.getString("roles"));
                    String newRoles;
                    if (rolesString == null || rolesString.trim().isEmpty()) {
                        newRoles = sanitize(role);
                    } else {
                        String[] rolesArray = rolesString.split(",\\s*");
                        List<String> rolesList = new ArrayList<>();
                        boolean alreadyPresent = false;
                        for (String r : rolesArray) {
                            if (r.equalsIgnoreCase(sanitize(role))) {
                                alreadyPresent = true;
                            }
                            rolesList.add(r);
                        }
                        if (alreadyPresent) {
                            return;
                        }
                        rolesList.add(sanitize(role));
                        newRoles = String.join(", ", rolesList);
                    }
                    String updateQuery = "UPDATE cse360users SET roles = ? WHERE userName = ?";
                    try (PreparedStatement updatePstmt = connection.prepareStatement(updateQuery)) {
                        updatePstmt.setString(1, sanitize(newRoles));
                        updatePstmt.setString(2, sanitize(username));
                        updatePstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // filters and removes role, then puts it back together
    public void removeUserRole(String username, String role) {
        String query = "SELECT roles FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(username));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String rolesString = sanitize(rs.getString("roles"));
                    String newRoles;
                    if (rolesString == null || rolesString.trim().isEmpty()) {
                        return;
                    } else {
                        String[] rolesArray = rolesString.split(",\\s*");
                        List<String> rolesList = new ArrayList<>();
                        for (String r : rolesArray) {
                            if (r.equalsIgnoreCase(sanitize(role))) {
                                continue;
                            }
                            rolesList.add(r);
                        }
                        newRoles = String.join(", ", rolesList);
                    }
                    String updateQuery = "UPDATE cse360users SET roles = ? WHERE userName = ?";
                    try (PreparedStatement updatePstmt = connection.prepareStatement(updateQuery)) {
                        updatePstmt.setString(1, sanitize(newRoles));
                        updatePstmt.setString(2, sanitize(username));
                        updatePstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // adds username and OPT to database
    public void addUserOTP(String username, String tempPassword) {
        String insertOrUpdateUserOTP = "MERGE INTO UserOTP (userName, tempPassword, isUsed) KEY (userName) VALUES (?, ?, FALSE)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertOrUpdateUserOTP)) {
            pstmt.setString(1, sanitize(username));
            pstmt.setString(2, sanitize(tempPassword));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // makes sure the user has OTP
    public boolean isUserInOPT(String userName) {
        String query = "SELECT COUNT(*) FROM UserOTP WHERE userName = ? AND isUsed = FALSE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(userName));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // checks OTP correctness
    public boolean validateUserOTP(String userName, String tempPassword) {
        String query = "SELECT * FROM UserOTP WHERE userName = ? AND tempPassword = ? AND isUsed = FALSE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(userName));
            pstmt.setString(2, sanitize(tempPassword));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                markUserOTPAsUsed(sanitize(userName));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // removes OTP code
    public void markUserOTPAsUsed(String userName) {
        String query = "UPDATE UserOTP SET isUsed = TRUE WHERE userName = ? AND isUsed = FALSE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(userName));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

// methods for questions and answers
    
    // adds all of the question info, default values will be null for optionals
    public boolean insertQuestion(Question q) {
        String query = "INSERT INTO Questions (id, author, questionTitle, questionText, referencedQuestionId, timestamp, resolved) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, q.getId());
            pstmt.setString(2, sanitize(q.getAuthor()));
            pstmt.setString(3, sanitize(q.getTitle()));
            pstmt.setString(4, sanitize(q.getQuestionText()));
            if(q.getReferencedQuestion() != null) {
                pstmt.setObject(5, q.getReferencedQuestion().getId());
            } else {
                pstmt.setObject(5, null);
            }
            pstmt.setTimestamp(6, Timestamp.valueOf(q.getTimestamp()));
            pstmt.setBoolean(7, q.isResolved());
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // same as as question but it also has links to questions
    public boolean insertAnswer(Answer a, UUID questionId) {
        String query = "INSERT INTO Answers (id, questionId, answerText, author, isApprovedSolution, timestamp, parentAnswerId) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, a.getId());
            pstmt.setObject(2, questionId);
            pstmt.setString(3, sanitize(a.getAnswerText()));
            pstmt.setString(4, sanitize(a.getAuthor()));
            pstmt.setBoolean(5, a.isApprovedSolution());
            pstmt.setTimestamp(6, Timestamp.valueOf(a.getTimestamp()));
            pstmt.setObject(7, a.getParentAnswerId());
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // updates question in database
    public boolean updateQuestion(Question q) {
    	// creates a query to securely retrieve information from the database where the parameters match 
        String query = "UPDATE Questions SET author = ?, questionTitle = ?, questionText = ?, referencedQuestionId = ?, timestamp = ?, resolved = ? WHERE id = ?";
        // user preparedStatement with query to safely check information
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(q.getAuthor())); // sanitizing the inputs (trims, etc.)
            pstmt.setString(2, sanitize(q.getTitle()));
            pstmt.setString(3, sanitize(q.getQuestionText()));
            if(q.getReferencedQuestion() != null) { // conditionally check if a reference exists
                pstmt.setObject(4, q.getReferencedQuestion().getId()); // set reference
            } else {
                pstmt.setObject(4, null); // else set null
            }
            pstmt.setTimestamp(5, Timestamp.valueOf(q.getTimestamp())); // retrieve other parameters
            pstmt.setBoolean(6, q.isResolved());
            pstmt.setObject(7, q.getId());
            int rows = pstmt.executeUpdate();
            return rows > 0; // if changes were updated rows is > 0
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // same thing
    public boolean updateAnswer(Answer a) {
        String query = "UPDATE Answers SET answerText = ?, author = ?, isApprovedSolution = ?, timestamp = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(a.getAnswerText()));
            pstmt.setString(2, sanitize(a.getAuthor()));
            pstmt.setBoolean(3, a.isApprovedSolution());
            pstmt.setTimestamp(4, Timestamp.valueOf(a.getTimestamp()));
            pstmt.setObject(5, a.getId());
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // deletes from id number
    public boolean deleteQuestion(UUID questionId) {
        String query = "DELETE FROM Questions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, questionId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // same
    public boolean deleteAnswer(UUID answerId) {
        String query = "DELETE FROM Answers WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, answerId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // returns list of all the question objects (for initialization)
    public List<Question> getAllQuestionsFromDB() {
        List<Question> questions = new ArrayList<>();
        String query = "SELECT * FROM Questions";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                try { // try catch to make it so if one breaks it doesn't stop
                    UUID id = (UUID) rs.getObject("id");
                    String author = sanitize(rs.getString("author"));
                    String questionTitle = sanitize(rs.getString("questionTitle"));
                    String questionText = sanitize(rs.getString("questionText"));
                    Timestamp ts = rs.getTimestamp("timestamp");
                    LocalDateTime ldt = ts.toLocalDateTime();
                    boolean resolved = rs.getBoolean("resolved");
                    Question q = new Question(author, questionTitle, questionText, ldt, id, resolved);
                    questions.add(q);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questions;
    }
    
    // gets all answers for a particular question
    public List<Answer> getAnswersForQuestion(UUID questionId) {
        List<Answer> answers = new ArrayList<>();
        String query = "SELECT * FROM Answers WHERE questionId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID id = (UUID) rs.getObject("id");
                    String answerText = sanitize(rs.getString("answerText"));
                    String author = sanitize(rs.getString("author"));
                    boolean isApprovedSolution = rs.getBoolean("isApprovedSolution");
                    Timestamp ts = rs.getTimestamp("timestamp");
                    LocalDateTime ldt = ts.toLocalDateTime();
                    Answer a = new Answer(answerText, author, ldt, id, isApprovedSolution);
                    UUID parentId = (UUID) rs.getObject("parentAnswerId");
                    a.setParentAnswerId(parentId);
                    answers.add(a);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return answers;
    }
    
    // inserts username into reviewer request table
    public boolean addReviewerRequest(String username) {
        String query = "INSERT INTO ReviewerRequests (id, requestUser, isApproved) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, UUID.randomUUID());
            pstmt.setString(2, sanitize(username));
            pstmt.setNull(3, Types.BOOLEAN);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // sets true or false to reviewer request and updates their permissions as needed
    public boolean updateReviewerRequest(String username, boolean isApproved) {
        String query = "UPDATE ReviewerRequests SET isApproved = ? WHERE requestUser = ?"; 
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setBoolean(1, isApproved);
            pstmt.setString(2, sanitize(username));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // returns list of requests
    public List<ReviewerRequest> getReviewerRequests() {
        List<ReviewerRequest> list = new ArrayList<>();
        String query = "SELECT * FROM ReviewerRequests WHERE isApproved IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String username = rs.getString("requestUser");
                Boolean isApproved = (Boolean) rs.getObject("isApproved");
                list.add(new ReviewerRequest(id, username, isApproved));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    // creates new review
    public boolean insertReview(Review review) {
        String query = "INSERT INTO Reviews (id, reviewText, reviewer, answerId, timestamp, privateFeedbackCount, previousReviewId) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, review.getId());
            pstmt.setString(2, review.getReviewText());
            pstmt.setString(3, review.getReviewer());
            pstmt.setObject(4, review.getAnswerId());
            pstmt.setTimestamp(5, Timestamp.valueOf(review.getTimestamp()));
            pstmt.setInt(6, review.getPrivateFeedbackCount());
            if (review.getPreviousReviewId() != null) {
                pstmt.setObject(7, review.getPreviousReviewId());
            } else {
                pstmt.setNull(7, Types.OTHER);
            }
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // updates review
    public boolean updateReview(Review review) {
        String query = "UPDATE Reviews SET reviewText = ?, timestamp = ?, privateFeedbackCount = ?, previousReviewId = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, review.getReviewText());
            pstmt.setTimestamp(2, Timestamp.valueOf(review.getTimestamp()));
            pstmt.setInt(3, review.getPrivateFeedbackCount());
            if (review.getPreviousReviewId() != null) {
                pstmt.setObject(4, review.getPreviousReviewId());
            } else {
                pstmt.setNull(4, Types.OTHER);
            }
            pstmt.setObject(5, review.getId());
            int rows = pstmt.executeUpdate();
            System.out.println("Rows updated: " + rows);
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // searches using UUID for specific review
    public Review getReviewById(UUID id) {
        String query = "SELECT * FROM Reviews WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String reviewText = rs.getString("reviewText");
                String reviewer = rs.getString("reviewer");
                UUID answerId = (UUID) rs.getObject("answerId");
                Timestamp ts = rs.getTimestamp("timestamp");
                LocalDateTime timestamp = ts.toLocalDateTime();
                int feedbackCount = rs.getInt("privateFeedbackCount");
                UUID previousReviewId = (UUID) rs.getObject("previousReviewId");
                Review review = new Review(reviewText, reviewer, answerId, previousReviewId);
                review.setTimestamp(timestamp);
                review.setPrivateFeedbackCount(feedbackCount);
                return review;
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // returns all reviews by reviewer
    public List<Review> getReviewsByReviewer(String reviewer) {
        List<Review> reviewList = new ArrayList<>();
        String query = "SELECT * FROM Reviews WHERE reviewer = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(reviewer));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String reviewText = rs.getString("reviewText");
                String reviewerName = rs.getString("reviewer");
                UUID answerId = (UUID) rs.getObject("answerId");
                Timestamp ts = rs.getTimestamp("timestamp");
                LocalDateTime timestamp = ts.toLocalDateTime();
                int feedbackCount = rs.getInt("privateFeedbackCount");
                UUID previousReviewId = (UUID) rs.getObject("previousReviewId");
                Review review = new Review(reviewText, reviewerName, answerId, previousReviewId);
                review.setPrivateFeedbackCount(feedbackCount);
                reviewList.add(review);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return reviewList;
    }
    
    // returns all reviews
    public List<Review> getAllReviews() {
        List<Review> reviews = new ArrayList<>();
        String query = "SELECT * FROM Reviews";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String reviewText = rs.getString("reviewText");
                String reviewer = rs.getString("reviewer");
                UUID answerId = (UUID) rs.getObject("answerId");
                Timestamp ts = rs.getTimestamp("timestamp");
                LocalDateTime timestamp = ts.toLocalDateTime();
                int feedbackCount = rs.getInt("privateFeedbackCount");
                UUID previousReviewId = (UUID) rs.getObject("previousReviewId");
                Review review = new Review(reviewText, reviewer, answerId, previousReviewId);
                review.setTimestamp(timestamp);
                review.setPrivateFeedbackCount(feedbackCount);
                reviews.add(review);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return reviews;
    }

    // private feedback system
    public boolean insertPrivateFeedback(String fromUser, String reviewer, String feedback) {
        String query = "INSERT INTO PrivateFeedback (id, fromUser, reviewer, feedback, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, UUID.randomUUID());
            pstmt.setString(2, sanitize(fromUser));
            pstmt.setString(3, sanitize(reviewer));
            pstmt.setString(4, sanitize(feedback));
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
 // adds reviewer (ensures correct permissions
    public boolean addApprovedReviewer(String ownerUserName, String reviewerName, double rating) {
        if(!hasRole(ownerUserName, "admin")) {
            System.err.println("You do not have permission to do this.");
            return false;
        }
        if(ownerUserName.equalsIgnoreCase(reviewerName)) {
            System.err.println("Cannot add yourself as a trusted reviewer.");
            return false;
        }
        Map<String, Double> existing = getApprovedReviewers(ownerUserName);
        if(existing.containsKey(reviewerName)) {
            System.err.println("Reviewer already exists in your trusted list.");
            return false;
        }
        String query = "INSERT INTO ApprovedReviewers (ownerUserName, reviewerName, reviewerRating) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(ownerUserName));
            pstmt.setString(2, sanitize(reviewerName));
            pstmt.setDouble(3, rating);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // get private feedback for a reviewer
    public List<PrivateFeedback> getPrivateFeedbackForReviewer(String reviewer) {
        List<PrivateFeedback> list = new ArrayList<>();
        String query = "SELECT * FROM PrivateFeedback WHERE reviewer = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(reviewer));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String fromUser = rs.getString("fromUser");
                String feedback = rs.getString("feedback");
                Timestamp ts = rs.getTimestamp("timestamp");
                LocalDateTime timestamp = ts.toLocalDateTime();
                list.add(new PrivateFeedback(id, fromUser, reviewer, feedback, timestamp));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    // creates new private message
    public boolean insertPrivateMessage(PrivateMessage msg) {
        String query = "INSERT INTO PrivateMessages (id, sender, recipient, subject, message, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, msg.getId());
            pstmt.setString(2, sanitize(msg.getSender()));
            pstmt.setString(3, sanitize(msg.getRecipient()));
            pstmt.setString(4, sanitize(msg.getSubject()));
            pstmt.setString(5, sanitize(msg.getMessage()));
            pstmt.setTimestamp(6, Timestamp.valueOf(msg.getTimestamp()));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // retrieve all private messages for a given recipient
    public List<PrivateMessage> getPrivateMessagesForUser(String recipient) {
        List<PrivateMessage> messages = new ArrayList<>();
        String query = "SELECT * FROM PrivateMessages WHERE recipient = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, sanitize(recipient));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String sender = rs.getString("sender");
                String subject = rs.getString("subject");
                String message = rs.getString("message");
                Timestamp ts = rs.getTimestamp("timestamp");
                LocalDateTime timestamp = ts.toLocalDateTime();
                messages.add(new PrivateMessage(id, sender, recipient, subject, message, timestamp));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    
    // all private messages from the database
    public List<PrivateMessage> getAllPrivateMessages() {
        List<PrivateMessage> messages = new ArrayList<>();
        String query = "SELECT * FROM PrivateMessages";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String sender = rs.getString("sender");
                String recipient = rs.getString("recipient");
                String subject = rs.getString("subject");
                String message = rs.getString("message");
                Timestamp ts = rs.getTimestamp("timestamp");
                LocalDateTime timestamp = ts.toLocalDateTime();
                messages.add(new PrivateMessage(id, sender, recipient, subject, message, timestamp));
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    
    // adds question UUID to flagged table
    public boolean flagQuestion(UUID questionId, String flaggedBy) {
        String query = "INSERT INTO FlaggedQuestions (questionId, flaggedBy, flagTimestamp) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, questionId);
            pstmt.setString(2, sanitize(flaggedBy));
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // removes question ID from flagged table
    public boolean unflagQuestion(UUID questionId) {
        String query = "DELETE FROM FlaggedQuestions WHERE questionId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, questionId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // returns if a question is flagged
    public boolean isQuestionFlagged(UUID questionId) {
        String query = "SELECT COUNT(*) FROM FlaggedQuestions WHERE questionId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()){
                return rs.getInt(1) > 0;
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // same as question
    public boolean flagAnswer(UUID answerId, String flaggedBy) {
        String query = "INSERT INTO FlaggedAnswers (answerId, flaggedBy, flagTimestamp) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, answerId);
            pstmt.setString(2, sanitize(flaggedBy));
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // same as question
    public boolean unflagAnswer(UUID answerId) {
        String query = "DELETE FROM FlaggedAnswers WHERE answerId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, answerId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // same as question
    public boolean isAnswerFlagged(UUID answerId) {
        String query = "SELECT COUNT(*) FROM FlaggedAnswers WHERE answerId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setObject(1, answerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()){
                return rs.getInt(1) > 0;
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // returns all flagged questions ID
    public List<UUID> getAllFlaggedQuestionIds() {
        List<UUID> ids = new ArrayList<>();
        String query = "SELECT questionId FROM FlaggedQuestions";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ids.add((UUID) rs.getObject("questionId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    // returns all flagged answers ID
    public List<UUID> getAllFlaggedAnswerIds() {
        List<UUID> ids = new ArrayList<>();
        String query = "SELECT answerId FROM FlaggedAnswers";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ids.add((UUID) rs.getObject("answerId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public void closeConnection() {
        try {
            if(statement != null) statement.close();
        } catch(SQLException se2) {
            se2.printStackTrace();
        }
        try {
            if(connection != null) connection.close();
        } catch(SQLException se) {
            se.printStackTrace();
        }
    }
}
