package com.beeflirt.beetalk.models;

import com.google.gson.annotations.SerializedName;

public class GalleryPost {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("photo_url")
    private String photoUrl;

    @SerializedName("caption")
    private String caption;

    @SerializedName("name")
    private String name;

    @SerializedName("username")
    private String username;

    @SerializedName("user_photo")
    private String userPhoto;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("like_count")
    private int likeCount;

    @SerializedName("is_liked")
    private int isLiked;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public String getPhotoUrl() { return photoUrl; }
    public String getCaption() { return caption; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getUserPhoto() { return userPhoto; }
    public String getCreatedAt() { return createdAt; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getIsLiked() { return isLiked; }
    public void setIsLiked(int isLiked) { this.isLiked = isLiked; }
}
