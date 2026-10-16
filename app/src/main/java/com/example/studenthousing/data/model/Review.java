package com.example.studenthousing.data.model;

import com.google.firebase.firestore.DocumentId;

public class Review {
    @DocumentId
    private String reviewId;
    private String requestId;
    private String reviewerId;
    private String revieweeId;
    private String reviewerName;
    private int rating;
    private String comment;
    private String imageUri;
    private long createdAt;

    public Review() {}

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getRevieweeId() { return revieweeId; }
    public void setRevieweeId(String revieweeId) { this.revieweeId = revieweeId; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
