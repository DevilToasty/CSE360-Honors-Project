package application;

import databasePart1.DatabaseHelper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReviewManager {

    private static List<Review> reviews = new ArrayList<>();

    public static void addReview(Review review, DatabaseHelper dbHelper) {
        reviews.add(review);
        dbHelper.insertReview(review);
    }

    public static void updateReview(Review review, String newReviewText, DatabaseHelper dbHelper) {

        review.setReviewText(newReviewText);
        review.setTimestamp(LocalDateTime.now());
        

        boolean updated = dbHelper.updateReview(review);
        System.out.println("Update returned: " + updated);
        
        // remove and reload the review from the database
        reviews.removeIf(r -> r.getId().equals(review.getId()));
        Review updatedReview = dbHelper.getReviewById(review.getId());
        if (updatedReview != null) {
            reviews.add(updatedReview);
        }
    }

    // custom filter methods using stream and data mutations
    public static List<Review> getReviewsByReviewer(String reviewer) {
        return reviews.stream()
                      .filter(r -> r.getReviewer().equals(reviewer))
                      .collect(Collectors.toList());
    }
    
    // custom filter methods using stream and data mutations
    public static List<Review> getReviewsForAnswer(String answerIdString) {
        return reviews.stream()
                      .filter(r -> r.getAnswerId().toString().equals(answerIdString))
                      .collect(Collectors.toList());
    }
    
    // database helper method to add feedback
    public static void addPrivateFeedback(Review review, DatabaseHelper dbHelper) {
        review.setPrivateFeedbackCount(review.getPrivateFeedbackCount() + 1);
        dbHelper.updateReview(review);
    }
}
