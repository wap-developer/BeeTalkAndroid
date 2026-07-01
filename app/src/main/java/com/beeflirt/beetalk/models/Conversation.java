package com.beeflirt.beetalk.models;

import com.google.gson.annotations.SerializedName;

public class Conversation {
    
    @SerializedName("match_id")
    private int matchId;
    
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("primary_photo")
    private String primaryPhoto;
    
    @SerializedName("last_message")
    private String lastMessage;
    
    @SerializedName("last_message_time")
    private String lastMessageTime;
    
    @SerializedName("unread_count")
    private int unreadCount;

    @SerializedName("is_online")
    private int isOnline;

    @SerializedName("last_seen")
    private String lastSeen;
    
    // Constructor
    public Conversation() {
    }
    
    // Getters and Setters
    public int getMatchId() {
        return matchId;
    }
    
    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPrimaryPhoto() {
        return primaryPhoto;
    }
    
    public void setPrimaryPhoto(String primaryPhoto) {
        this.primaryPhoto = primaryPhoto;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public String getLastMessageTime() {
        return lastMessageTime;
    }
    
    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(int isOnline) {
        this.isOnline = isOnline;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }
}
