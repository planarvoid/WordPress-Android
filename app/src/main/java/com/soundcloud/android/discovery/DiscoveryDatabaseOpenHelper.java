package com.soundcloud.android.discovery;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DiscoveryDatabaseOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    @Inject
    DiscoveryDatabaseOpenHelper(Context context) {
        super(context, "discovery.db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DbModel.SelectionItem.CREATE_TABLE);
        sqLiteDatabase.execSQL(DbModel.SingleContentSelectionCard.CREATE_TABLE);
        sqLiteDatabase.execSQL(DbModel.MultipleContentSelectionCard.CREATE_TABLE);
        sqLiteDatabase.execSQL(DbModel.DiscoveryCard.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
}
