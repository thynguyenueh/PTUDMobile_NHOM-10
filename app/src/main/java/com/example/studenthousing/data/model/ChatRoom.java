package com.example.studenthousing.data.model;

import com.google.firebase.firestore.DocumentId;
import java.util.List;

public class ChatRoom {
    @DocumentId
    private String roomId;
    private String postId;
    private String postTitle;
    private List<String> participantIds; // [userId1, userId2]
    private String lastMessage;
    private long lastMessageTime;
    private String lastSenderId;
    private long createdAt;

    public ChatRoom() {}

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
