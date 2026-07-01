package com.beeflirt.beetalk.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.beeflirt.beetalk.db.entity.ProfileEntity;

@Dao
public interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId")
    ProfileEntity getByUserId(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProfileEntity profile);

    @Query("DELETE FROM profiles WHERE userId = :userId")
    void deleteByUserId(int userId);
}
