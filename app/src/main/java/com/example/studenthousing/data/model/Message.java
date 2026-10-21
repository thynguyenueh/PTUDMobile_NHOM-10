package com.example.studenthousing.data.model;

import com.example.studenthousing.util.Constants;
import com.google.firebase.firestore.DocumentId;

public class Message {
    @DocumentId
    private String messageId;
    private String roomId;
    private String senderId;
    private String content;
    private String type;      // TEXT | IMAGE | SYSTEM
    private String imageUrl;
    private long sentAt;
    private boolean isRead;
    private String syncStatus = Constants.SYNC_SYNCED;

    public Message() {}

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getSentAt() { return sentAt; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
