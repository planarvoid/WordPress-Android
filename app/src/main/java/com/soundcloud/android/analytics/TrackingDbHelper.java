package com.soundcloud.android.analytics;

import com.soundcloud.propeller.schema.Table;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import javax.inject.Inject;

class TrackingDbHelper extends SQLiteOpenHelper {

    static final Table EVENTS_TABLE = new Table() {
        @Override
        public String name() {
            return "events";
        }

        @Override
        public PrimaryKey primaryKey() {
            return PrimaryKey.of(TrackingColumns._ID);
        }
    };

    private static final String DATABASE_NAME = "SoundCloud-tracking.sqlite";
    private static final int DATABASE_VERSION = 5;

    static final String DATABASE_CREATE_EVENTS_TABLE = "CREATE TABLE IF NOT EXISTS " + EVENTS_TABLE.name() + "(" +
            TrackingColumns._ID + " INTEGER PRIMARY KEY," +
            TrackingColumns.TIMESTAMP + " INTEGER NOT NULL," +
            TrackingColumns.BACKEND + " STRING NOT NULL," +
            TrackingColumns.DATA + " STRING NOT NULL," +
            "UNIQUE (" + TrackingColumns.TIMESTAMP + ", " + TrackingColumns.BACKEND + ", " + TrackingColumns.DATA + ") ON CONFLICT IGNORE" + ")";

    @Inject
    TrackingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_EVENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for now just recreate table and drop any local tracking events
        onRecreateDb(db);
    }

    private void onRecreateDb(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE.name());
        onCreate(db);
    }

    abstract class TrackingColumns implements BaseColumns {
        static final String TIMESTAMP = "timestamp";
        static final String BACKEND = "backend";
        static final String DATA = "data";
    }

}
