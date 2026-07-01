package com.beeflirt.beetalk.models;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("id")
    private int id;
    
    @SerializedName("match_id")
    private int matchId;
    
    @SerializedName("sender_id")
    private int senderId;
    
    @SerializedName("message")
    private String message;

    @SerializedName("message_type")
    private String messageType;

    @SerializedName("media_url")
    private String mediaUrl;

    @SerializedName("is_read")
    private int isRead;
    
    @SerializedName("created_at")
    private String createdAt;
    
    public Message() {
    }
    
    public Message(int matchId, int senderId, String message) {
        this.matchId = matchId;
        this.senderId = senderId;
        this.message = message;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getMatchId() {
        return matchId;
    }
    
    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }
    
    public int getSenderId() {
        return senderId;
    }
    
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getIsRead() {
        return isRead;
    }
    
    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isRead() {
        return isRead == 1;
    }

    public String getMessageType() {
        return messageType != null ? messageType : "text";
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public boolean isImageMessage() {
        return "image".equals(getMessageType());
    }
}
