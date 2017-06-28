package com.soundcloud.android.discovery;

import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DiscoveryDatabaseOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 6;

    private final SyncStateStorage syncStateStorage;

    @Inject
    DiscoveryDatabaseOpenHelper(Context context, SyncStateStorage syncStateStorage) {
        super(context, "discovery.db", null, DATABASE_VERSION);
        this.syncStateStorage = syncStateStorage;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DbModel.SelectionItem.CREATE_TABLE);
        sqLiteDatabase.execSQL(DbModel.SingleContentSelectionCard.CREATE_TABLE);
        sqLiteDatabase.execSQL(DbModel.MultipleContentSelectionCard.CREATE_TABLE);
        sqLiteDatabase.execSQL(DbModel.DiscoveryCard.CREATE_TABLE);
        sqLiteDatabase.execSQL(DbModel.SystemPlaylist.CREATE_TABLE);
        sqLiteDatabase.execSQL(DbModel.SystemPlaylistsTracks.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // this probably won't work due to a race-condition with NewSyncOperations, but we should try anyways :)
        syncStateStorage.clear(Syncable.DISCOVERY_CARDS);
        try {
            db.beginTransaction();
            recreateDb(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Drops and recreates all discovery tables
     *
     * @param db
     */
    private void recreateDb(SQLiteDatabase db) {
        db.execSQL(DbModel.SelectionItem.DROPTABLE);
        db.execSQL(DbModel.SingleContentSelectionCard.DROPTABLE);
        db.execSQL(DbModel.MultipleContentSelectionCard.DROPTABLE);
        db.execSQL(DbModel.DiscoveryCard.DROPTABLE);
        db.execSQL(DbModel.SystemPlaylistsTracks.DROPTABLE);
        db.execSQL(DbModel.SystemPlaylist.DROPTABLE);
        onCreate(db);
    }
}
