package com.soundcloud.android.collection;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import javax.inject.Inject;

public class CollectionDatabaseOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;

    @Inject
    public CollectionDatabaseOpenHelper(Context context) {
        super(context, "collection.db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbModel.PlayHistory.CREATE_TABLE);
        db.execSQL(DbModel.RecentlyPlayed.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 2) {
            db.execSQL(DbModel.RecentlyPlayed.CREATE_TABLE);
        }
    }
}
