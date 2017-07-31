package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.storage.SqlBriteDatabase;
import io.reactivex.Scheduler;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class DiscoveryDatabase extends SqlBriteDatabase {

    @Inject
    DiscoveryDatabase(DiscoveryDatabaseOpenHelper discoveryDatabaseOpenHelper,
                      @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        super(discoveryDatabaseOpenHelper, scheduler);
    }

    void cleanUp() throws SQLException {
        runInTransaction(() -> {
            final SQLiteDatabase writableDatabase = briteDatabase.getWritableDatabase();
            writableDatabase.execSQL(DbModel.SelectionItem.DELETEALL);
            writableDatabase.execSQL(DbModel.SingleContentSelectionCard.DELETEALL);
            writableDatabase.execSQL(DbModel.MultipleContentSelectionCard.DELETEALL);
            writableDatabase.execSQL(DbModel.DiscoveryCard.DELETEALL);
            writableDatabase.execSQL(DbModel.SystemPlaylistsTracks.DELETEALL);
            writableDatabase.execSQL(DbModel.SystemPlaylist.DELETEALL);
        });
    }

}
