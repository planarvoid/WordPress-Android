package com.soundcloud.android.framework;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentUpdates;
import com.soundcloud.android.offline.OfflineDatabase;
import com.soundcloud.android.offline.OfflineDatabaseOpenHelper;
import com.soundcloud.android.offline.TrackDownloadsStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.SchemaMigrationHelper;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.TrackPolicies;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import io.reactivex.schedulers.Schedulers;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.Collections;

public class IntegrationTestsFixtures {

    public void insertOfflineTrack(Context context, Urn track) {
        insert(context, TrackPolicies.TABLE.name(), buildTrackPoliciesValues(track, System.currentTimeMillis()));
        getTrackDownloadsStorage(context)
                .writeUpdates(OfflineContentUpdates.builder().newTracksToDownload(Collections.singletonList(track)).build())
                .test()
                .assertComplete();
    }

    @NonNull
    protected TrackDownloadsStorage getTrackDownloadsStorage(Context context) {
        return new TrackDownloadsStorage(new CurrentDateProvider(),
                                         new OfflineDatabase(new OfflineDatabaseOpenHelper(context), Schedulers.trampoline()));
    }

    public void clearLikes(Context context) {
        final SQLiteDatabase db = databaseManager(context).getWritableDatabase();
        SchemaMigrationHelper.recreateTable(Tables.Likes.TABLE, db);
    }

    private DatabaseManager databaseManager(Context context) {
        return DatabaseManager.getInstance(context, new ApplicationProperties(context.getResources()));
    }

    public void clearOfflineContent(Context context) {
        // clear downloads storage
        getTrackDownloadsStorage(context).deleteAllDownloads().test().assertComplete();

        // clear backing offline content preference
        final SharedPreferences offlineContentStorage = context.getSharedPreferences(StorageModule.OFFLINE_CONTENT, Context.MODE_PRIVATE);
        offlineContentStorage.edit().clear().commit();
    }

    private ContentValues buildTrackPoliciesValues(Urn track, long date) {
        return ContentValuesBuilder.values()
                                   .put(Tables.TrackPolicies.TRACK_ID, track.getNumericId())
                                   .put(Tables.TrackPolicies.POLICY, "POLICY TEST")
                                   .put(Tables.TrackPolicies.MONETIZABLE, true)
                                   .put(Tables.TrackPolicies.SYNCABLE, true)
                                   .put(Tables.TrackPolicies.LAST_UPDATED, date)
                                   .get();
    }

    public void updateLastPolicyUpdateTime(Context context, long time) {
        update(context, buildTrackPoliciesValues(time));
    }

    private void insert(Context context, String table, ContentValues contentValues) {
        final SQLiteDatabase db = databaseManager(context).getWritableDatabase();
        db.insert(table, null, contentValues);
    }

    private void update(Context context, ContentValues contentValues) {
        final SQLiteDatabase db = databaseManager(context).getWritableDatabase();
        db.update(TrackPolicies.TABLE.name(), contentValues, filter().build(), null);
    }

    private static ContentValues buildTrackPoliciesValues(long date) {
        return ContentValuesBuilder.values()
                                   .put(Tables.TrackPolicies.LAST_UPDATED, date)
                                   .get();
    }
}
