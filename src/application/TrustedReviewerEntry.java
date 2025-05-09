package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrustedReviewerEntry {
    private final StringProperty reviewer;
    private final StringProperty weight;

    public TrustedReviewerEntry(String reviewer, String weight) {
        this.reviewer = new SimpleStringProperty(reviewer);
        this.weight = new SimpleStringProperty(weight);
    }

    public String getReviewer() {
        return reviewer.get();
    }

    public void setReviewer(String reviewer) {
        this.reviewer.set(reviewer);
    }

    public StringProperty reviewerProperty() {
        return reviewer;
    }

    public String getWeight() {
        return weight.get();
    }

    public void setWeight(String weight) {
        this.weight.set(weight);
    }

    public StringProperty weightProperty() {
        return weight;
    }

    // create a map of reviewers and add values of their scores
    public static List<TrustedReviewerEntry> fromApprovedReviewers(Map<String, Double> approvedReviewers) {
        List<TrustedReviewerEntry> list = new ArrayList<>();
        for (Map.Entry<String, Double> entry : approvedReviewers.entrySet()) {
            list.add(new TrustedReviewerEntry(entry.getKey(), String.valueOf(entry.getValue())));
        }
        return list;
    }
}
