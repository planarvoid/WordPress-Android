package com.soundcloud.android.analytics.eventlogger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import javax.inject.Inject;

class EventLoggerDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SoundCloud-tracking.sqlite";
    private static final int DATABASE_VERSION = 3;

    static final String EVENTS_TABLE = "events";

    @Inject
    EventLoggerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_PLAY_TRACKING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for now just recreate table and drop any local tracking events
        onRecreateDb(db);
    }

    private void onRecreateDb(SQLiteDatabase db){
        db.execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE);
        onCreate(db);
    }

    private static final String DATABASE_CREATE_PLAY_TRACKING = "CREATE TABLE IF NOT EXISTS " + EVENTS_TABLE + "(" +
            TrackingEvents._ID + " INTEGER PRIMARY KEY," +
            TrackingEvents.TIMESTAMP + " INTEGER NOT NULL," +
            TrackingEvents.PATH + " STRING NOT NULL," +
            TrackingEvents.PARAMS + " TEXT NOT NULL," +
            "UNIQUE (" + TrackingEvents.TIMESTAMP + ", " + TrackingEvents.PARAMS + ") ON CONFLICT IGNORE" +")";

    public interface TrackingEvents extends BaseColumns {
        String TIMESTAMP = "timestamp";
        String PATH = "path";
        String PARAMS = "params";
    }
}
