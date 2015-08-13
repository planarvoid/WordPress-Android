package com.soundcloud.android.framework;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Tables.OfflineContent._TYPE;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.SchemaMigrationHelper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.propeller.ContentValuesBuilder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class IntegrationTestsFixtures {
    public void insertLocalPlaylistWithTrack(Context context, Urn playlist, Urn track) {
        insert(context, Table.PlaylistTracks.name(), buildPlaylistTrackValues(playlist, track, System.currentTimeMillis()));
    }

    public void insertOfflinePlaylist(Context context, Urn parcelableExtra) {
        insert(context, OfflineContent.TABLE.name(), buildOfflineContentValues(parcelableExtra));
    }

    public void insertOfflineTrack(Context context, Urn track) {
        insert(context, TrackDownloads.TABLE.name(), buildTrackDownloadValues(track, System.currentTimeMillis()));
        insert(context, Table.TrackPolicies.name(), buildTrackPoliciesValues(track, System.currentTimeMillis()));
    }

    public void clearLikes(Context context) {
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        SchemaMigrationHelper.recreate(Table.Likes, db);
    }

    public void clearOfflineContent(Context context) {
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        db.delete(TrackDownloads.TABLE.name(), null, null);
        db.delete(OfflineContent.TABLE.name(), null, null);
    }

    private ContentValues buildTrackPoliciesValues(Urn track, long date) {
        return ContentValuesBuilder.values()
                .put(TableColumns.TrackPolicies.TRACK_ID, track.getNumericId())
                .put(TableColumns.TrackPolicies.POLICY, "POLICY TEST")
                .put(TableColumns.TrackPolicies.MONETIZABLE, true)
                .put(TableColumns.TrackPolicies.SYNCABLE, true)
                .put(TableColumns.TrackPolicies.LAST_UPDATED, date)
                .get();
    }

    public void updateLastPolicyUpdateTime(Context context, long time) {
        update(context, buildTrackPoliciesValues(time));
    }

    private void insert(Context context, String table, ContentValues contentValues) {
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        db.insert(table, null, contentValues);
    }

    private void update(Context context, ContentValues contentValues) {
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        db.update(Table.TrackPolicies.name(), contentValues, filter().build(), null);
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
                .put(TableColumns.TrackPolicies.LAST_UPDATED, date)
                .get();
    }
}
