package com.beeflirt.beetalk.db.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "matches", indices = {@Index("userId")})
public class MatchEntity {
    @PrimaryKey
    private int matchId;
    private int userId;
    private String name;
    private int age;
    private String gender;
    private String bio;
    private String city;
    private double latitude;
    private double longitude;
    private String primaryPhoto;
    private String lastMessage;
    private String lastMessageTime;
    private String matchedAt;
    private int unreadCount;
    private int isOnline;
    private String lastSeen;

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPrimaryPhoto() { return primaryPhoto; }
    public void setPrimaryPhoto(String primaryPhoto) { this.primaryPhoto = primaryPhoto; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public String getMatchedAt() { return matchedAt; }
    public void setMatchedAt(String matchedAt) { this.matchedAt = matchedAt; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public int getIsOnline() { return isOnline; }
    public void setIsOnline(int isOnline) { this.isOnline = isOnline; }

    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
}
