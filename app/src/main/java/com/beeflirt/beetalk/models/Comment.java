package com.beeflirt.beetalk.models;

import com.google.gson.annotations.SerializedName;

public class Comment {

    @SerializedName("id")
    private int id;

    @SerializedName("post_id")
    private int postId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("parent_comment_id")
    private Integer parentCommentId;

    @SerializedName("comment_text")
    private String commentText;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("user_name")
    private String userName;

    @SerializedName("user_photo")
    private String userPhoto;

    @SerializedName("like_count")
    private int likeCount;

    @SerializedName("is_liked")
    private int isLiked;

    @SerializedName("reply_count")
    private int replyCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Integer parentCommentId) { this.parentCommentId = parentCommentId; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhoto() { return userPhoto; }
    public void setUserPhoto(String userPhoto) { this.userPhoto = userPhoto; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getIsLiked() { return isLiked; }
    public void setIsLiked(int isLiked) { this.isLiked = isLiked; }

    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
}
