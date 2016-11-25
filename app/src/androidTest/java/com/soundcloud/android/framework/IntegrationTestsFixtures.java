package com.soundcloud.android.framework;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Tables.OfflineContent._TYPE;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.SchemaMigrationHelper;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.android.storage.Tables.TrackPolicies;
import com.soundcloud.propeller.ContentValuesBuilder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class IntegrationTestsFixtures {

    public void insertOfflineTrack(Context context, Urn track) {
        insert(context, TrackDownloads.TABLE.name(), buildTrackDownloadValues(track, System.currentTimeMillis()));
        insert(context, TrackPolicies.TABLE.name(), buildTrackPoliciesValues(track, System.currentTimeMillis()));
    }

    public void clearLikes(Context context) {
        final SQLiteDatabase db = databaseManager(context).getWritableDatabase();
        SchemaMigrationHelper.recreateTable(Tables.Likes.TABLE, db);
    }

    private DatabaseManager databaseManager(Context context) {
        return DatabaseManager.getInstance(context, new ApplicationProperties(context.getResources()));
    }

    public void clearOfflineContent(Context context) {
        final SQLiteDatabase db = databaseManager(context).getWritableDatabase();
        db.delete(TrackDownloads.TABLE.name(), null, null);
        db.delete(OfflineContent.TABLE.name(), null, null);
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

    private static ContentValues buildOfflineContentValues(Urn urn) {
        return ContentValuesBuilder.values(2)
                                   .put(_ID, urn.getNumericId())
                                   .put(_TYPE, OfflineContent.TYPE_PLAYLIST)
                                   .get();
    }

    private static ContentValues buildPlaylistTrackValues(Urn playlist, Urn trackUrn, long date) {
        return ContentValuesBuilder.values()
                                   .put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlist.getNumericId())
                                   .put(TableColumns.PlaylistTracks.TRACK_ID, trackUrn.getNumericId())
                                   .put(TableColumns.PlaylistTracks.POSITION, 0)
                                   .put(TableColumns.PlaylistTracks.ADDED_AT, date)
                                   .get();
    }

    private static ContentValues buildTrackDownloadValues(Urn track, long date) {
        return ContentValuesBuilder.values(2)
                                   .put(_ID, track.getNumericId())
                                   .put(TrackDownloads.REQUESTED_AT, date)
                                   .put(TrackDownloads.DOWNLOADED_AT, date)
                                   .get();
    }

    private static ContentValues buildTrackPoliciesValues(long date) {
        return ContentValuesBuilder.values()
                                   .put(Tables.TrackPolicies.LAST_UPDATED, date)
                                   .get();
    }
}
