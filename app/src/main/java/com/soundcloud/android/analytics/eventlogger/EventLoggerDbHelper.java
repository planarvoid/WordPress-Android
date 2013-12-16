package com.soundcloud.android.analytics.eventlogger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import javax.inject.Inject;

class EventLoggerDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SoundCloud-tracking.sqlite";
    private static final int DATABASE_VERSION = 2;

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
            TrackingEvents.ACTION + " TEXT NOT NULL," +
            TrackingEvents.SOUND_URN + " TEXT NOT NULL," +
            TrackingEvents.USER_URN + " TEXT NOT NULL," + // soundcloud:users:123 for logged in, soundcloud:users:0 for logged out
            TrackingEvents.SOUND_DURATION + " INTEGER NOT NULL," + // this it the total sound length in millis
            TrackingEvents.SOURCE_INFO + " TEXT NOT NULL," +
            "UNIQUE (" + TrackingEvents.TIMESTAMP + ", " + TrackingEvents.ACTION + ") ON CONFLICT IGNORE" +
            ")";

    public interface TrackingEvents extends BaseColumns {
        final String TIMESTAMP = "timestamp";
        final String ACTION    = "action";
        final String SOUND_URN = "sound_urn";
        final String USER_URN  = "user_urn";
        final String SOUND_DURATION = "sound_duration";
        final String SOURCE_INFO = "source_info";
    }
}
