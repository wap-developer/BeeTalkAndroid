package com.beeflirt.beetalk.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.beeflirt.beetalk.db.entity.ConversationEntity;

import java.util.List;

@Dao
public interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    List<ConversationEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ConversationEntity> conversations);

    @Query("DELETE FROM conversations")
    void deleteAll();
}
