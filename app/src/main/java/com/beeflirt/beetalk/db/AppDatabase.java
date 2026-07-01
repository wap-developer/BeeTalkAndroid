package com.beeflirt.beetalk.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.beeflirt.beetalk.db.dao.ConversationDao;
import com.beeflirt.beetalk.db.dao.MatchDao;
import com.beeflirt.beetalk.db.dao.MessageDao;
import com.beeflirt.beetalk.db.dao.PhotoDao;
import com.beeflirt.beetalk.db.dao.ProfileDao;
import com.beeflirt.beetalk.db.entity.ConversationEntity;
import com.beeflirt.beetalk.db.entity.MatchEntity;
import com.beeflirt.beetalk.db.entity.MessageEntity;
import com.beeflirt.beetalk.db.entity.PhotoEntity;
import com.beeflirt.beetalk.db.entity.ProfileEntity;

@Database(entities = {
        MessageEntity.class,
        ConversationEntity.class,
        MatchEntity.class,
        ProfileEntity.class,
        PhotoEntity.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MessageDao messageDao();
    public abstract ConversationDao conversationDao();
    public abstract MatchDao matchDao();
    public abstract ProfileDao profileDao();
    public abstract PhotoDao photoDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "beetalk_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
