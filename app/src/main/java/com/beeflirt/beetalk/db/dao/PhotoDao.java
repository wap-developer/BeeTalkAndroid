package com.beeflirt.beetalk.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.beeflirt.beetalk.db.entity.PhotoEntity;

import java.util.List;

@Dao
public interface PhotoDao {
    @Query("SELECT * FROM photos WHERE userId = :userId ORDER BY `order` ASC")
    List<PhotoEntity> getByUserId(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PhotoEntity> photos);

    @Query("DELETE FROM photos WHERE userId = :userId")
    void deleteByUserId(int userId);
}
