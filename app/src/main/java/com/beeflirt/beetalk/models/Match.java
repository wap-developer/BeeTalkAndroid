package com.beeflirt.beetalk.models;

import com.google.gson.annotations.SerializedName;

public class Match {
    @SerializedName("match_id")
    private int matchId;
    
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("age")
    private int age;
    
    @SerializedName("gender")
    private String gender;
    
    @SerializedName("bio")
    private String bio;
    
    @SerializedName("city")
    private String city;
    
    @SerializedName("latitude")
    private double latitude;
    
    @SerializedName("longitude")
    private double longitude;
    
    @SerializedName("primary_photo")
    private String primaryPhoto;
    
    @SerializedName("last_message")
    private String lastMessage;
    
    @SerializedName("last_message_time")
    private String lastMessageTime;
    
    @SerializedName("matched_at")
    private String matchedAt;
    
    @SerializedName("unread_count")
    private int unreadCount;

    @SerializedName("is_online")
    private int isOnline;

    @SerializedName("last_seen")
    private String lastSeen;
    
    public Match() {
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
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
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
    
    public String getMatchedAt() {
        return matchedAt;
    }
    
    public void setMatchedAt(String matchedAt) {
        this.matchedAt = matchedAt;
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
