package com.soundcloud.android.playback;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import javax.inject.Inject;

public class PlayQueueDatabaseOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    @Inject
    public PlayQueueDatabaseOpenHelper(Context context) {
        super(context, "play_queue.db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbModel.PlayQueue.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
