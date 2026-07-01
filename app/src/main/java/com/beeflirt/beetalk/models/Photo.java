package com.beeflirt.beetalk.models;

import com.google.gson.annotations.SerializedName;

public class Photo {
    @SerializedName("id")
    private int id;
    
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("photo_url")
    private String photoUrl;
    
    @SerializedName("is_primary")
    private int isPrimary;
    
    @SerializedName("order")
    private int order;
    
    public Photo() {
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getPhotoUrl() {
        return photoUrl;
    }
    
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
    
    public int getIsPrimary() {
        return isPrimary;
    }
    
    public void setIsPrimary(int isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public boolean isPrimary() {
        return isPrimary == 1;
    }
}
