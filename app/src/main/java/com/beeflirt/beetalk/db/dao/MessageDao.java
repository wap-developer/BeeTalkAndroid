package com.beeflirt.beetalk.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.beeflirt.beetalk.db.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages WHERE matchId = :matchId ORDER BY createdAt ASC")
    List<MessageEntity> getByMatchId(int matchId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MessageEntity> messages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MessageEntity message);

    @Query("DELETE FROM messages WHERE matchId = :matchId")
    void deleteByMatchId(int matchId);

    @Query("UPDATE messages SET isRead = 1 WHERE matchId = :matchId")
    void markAllRead(int matchId);
}
