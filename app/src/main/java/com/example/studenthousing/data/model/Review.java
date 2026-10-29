package com.example.studenthousing.data.model;

import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

public class Review {
    @DocumentId
    private String reviewId;
    private String reviewerId;
    private String revieweeId;
    private String reviewerName;
    private String revieweeName;
    private double rating;
    private String comment;
    private List<String> imageUrls = new ArrayList<>();
    private long createdAt;
    private long updatedAt;
    private String syncStatus = Constants.SYNC_SYNCED;

    public Review() {}

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getRevieweeId() { return revieweeId; }
    public void setRevieweeId(String revieweeId) { this.revieweeId = revieweeId; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public String getRevieweeName() { return revieweeName; }
    public void setRevieweeName(String revieweeName) { this.revieweeName = revieweeName; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
