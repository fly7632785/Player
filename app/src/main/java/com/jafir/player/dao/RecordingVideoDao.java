package com.jafir.player.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.jafir.player.RecordingModel;

import java.util.List;

import io.reactivex.Single;


@Dao
public interface RecordingVideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateRecordVideo(RecordingModel... channelTypeModels);

    @Query("SELECT * FROM recording_video  ORDER BY createTime DESC")
    Single<List<RecordingModel>> getAll();

    @Delete
    void delete(RecordingModel... channelTypeModels);
}
