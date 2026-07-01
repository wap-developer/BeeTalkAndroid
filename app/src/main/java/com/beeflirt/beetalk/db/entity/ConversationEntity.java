package com.beeflirt.beetalk.db.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations", indices = {@Index("userId")})
public class ConversationEntity {
    @PrimaryKey
    private int matchId;
    private int userId;
    private String name;
    private String primaryPhoto;
    private String lastMessage;
    private String lastMessageTime;
    private int unreadCount;
    private int isOnline;
    private String lastSeen;

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrimaryPhoto() { return primaryPhoto; }
    public void setPrimaryPhoto(String primaryPhoto) { this.primaryPhoto = primaryPhoto; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public int getIsOnline() { return isOnline; }
    public void setIsOnline(int isOnline) { this.isOnline = isOnline; }

    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
}
