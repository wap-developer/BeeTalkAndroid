package com.beeflirt.beetalk.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "photos",
        foreignKeys = @ForeignKey(
                entity = ProfileEntity.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("userId")})
public class PhotoEntity {
    @PrimaryKey
    private int id;
    private int userId;
    private String photoUrl;
    private int isPrimary;
    private int order;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public int getIsPrimary() { return isPrimary; }
    public void setIsPrimary(int isPrimary) { this.isPrimary = isPrimary; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
