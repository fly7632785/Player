package com.jafir.player.dao;

import android.app.Application;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;

import com.jafir.player.RecordingModel;


@Database(
        entities = {
                RecordingModel.class,
        },
        version = 1
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract RecordingVideoDao recordingVideoDao();

    public static AppDatabase create(Application application) {
        return Room.databaseBuilder(application, AppDatabase.class, "jafir.db")
                .build();
    }
}


