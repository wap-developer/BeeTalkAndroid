package com.beeflirt.beetalk.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.beeflirt.beetalk.db.entity.MatchEntity;

import java.util.List;

@Dao
public interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY matchedAt DESC")
    List<MatchEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MatchEntity> matches);

    @Query("DELETE FROM matches WHERE matchId = :matchId")
    void deleteByMatchId(int matchId);

    @Query("DELETE FROM matches")
    void deleteAll();
}
