package com.beeflirt.beetalk.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private int id;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("phone")
    private String phone;
    
    @SerializedName("is_verified")
    private int isVerified;
    
    @SerializedName("is_active")
    private int isActive;
    
    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("is_online")
    private int isOnline;

    @SerializedName("last_seen")
    private String lastSeen;

    @SerializedName("is_premium")
    private int isPremium;

    @SerializedName("premium_until")
    private String premiumUntil;

    @SerializedName("premium_source")
    private String premiumSource;

    public User() {
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public boolean isVerified() {
        return isVerified == 1;
    }
    
    public void setVerified(int verified) {
        isVerified = verified;
    }
    
    public boolean isActive() {
        return isActive == 1;
    }
    
    public void setActive(int active) {
        isActive = active;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
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

    public boolean isPremium() {
        return isPremium == 1;
    }

    public void setIsPremium(int isPremium) {
        this.isPremium = isPremium;
    }

    public String getPremiumUntil() {
        return premiumUntil;
    }

    public void setPremiumUntil(String premiumUntil) {
        this.premiumUntil = premiumUntil;
    }

    public String getPremiumSource() {
        return premiumSource;
    }

    public void setPremiumSource(String premiumSource) {
        this.premiumSource = premiumSource;
    }
}
