package com.soundcloud.android.backdoor;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.TableColumns.OfflineContent._TYPE;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;

public class IntegrationTestsBroadcastReceiver extends BroadcastReceiver {

    public static final String PLAYLIST = "playlist";
    public static final String TRACK = "track";
    public static final String TIME = "time";

    public enum Action {
        ADD_LOCAL_PLAYLIST_WITH_TRACK,
        SET_PLAYLIST_OFFLINE,
        SET_TRACK_OFFLINE,
        SET_LAST_POLICY_UPDATE

    }

    @Inject
    public IntegrationTestsBroadcastReceiver() {
        // Required by Dagger
    }

    IntentFilter getIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        for (Action action : Action.values()) {
            intentFilter.addAction(action.name());
        }
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (Action.valueOf(intent.getAction())) {
            case ADD_LOCAL_PLAYLIST_WITH_TRACK:
                addLocalPlaylistWithTrack(context, intent);
                break;
            case SET_PLAYLIST_OFFLINE:
                setOfflinePlaylist(context, intent);
                break;
            case SET_TRACK_OFFLINE:
                setOfflineTrack(context, intent);
                break;
            case SET_LAST_POLICY_UPDATE:
                setLastPolicyUpdateTime(context, intent);
                break;
            default:
                throw new UnsupportedOperationException("Unknown intent action: " + intent);
        }
    }

    private void addLocalPlaylistWithTrack(Context context, Intent intent) {
        Urn playlist = intent.getParcelableExtra(PLAYLIST);
        Urn track = intent.getParcelableExtra(TRACK);

        insert(context, Table.PlaylistTracks, buildPlaylistTrackValues(playlist, track, System.currentTimeMillis()));
    }

    private void setOfflinePlaylist(Context context, Intent intent) {
        insert(context, Table.OfflineContent, buildOfflineContentValues(intent.<Urn>getParcelableExtra(PLAYLIST)));
    }

    private void setOfflineTrack(Context context, Intent intent) {
        Urn track = intent.getParcelableExtra(TRACK);

        insert(context, Table.TrackDownloads, buildTrackDownloadValues(track, System.currentTimeMillis()));
        insert(context, Table.TrackPolicies, buildTrackPoliciesValues(track, System.currentTimeMillis()));
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

    private void setLastPolicyUpdateTime(Context context, Intent intent) {
        long time = intent.getLongExtra(TIME, 0);
        update(context, buildTrackPoliciesValues(time));
    }

    private void insert(Context context, Table table, ContentValues contentValues) {
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        db.insert(table.name(), null, contentValues);
    }

    private void update(Context context, ContentValues contentValues) {
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        db.update(Table.TrackPolicies.name(), contentValues, filter().build(), null);
    }

    private static ContentValues buildOfflineContentValues(Urn urn) {
        return ContentValuesBuilder.values(2)
                .put(_ID, urn.getNumericId())
                .put(_TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST)
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

    public static ContentValues buildTrackDownloadValues(Urn track, long date) {
        return ContentValuesBuilder.values(2)
                .put(_ID, track.getNumericId())
                .put(REQUESTED_AT, date)
                .put(DOWNLOADED_AT, date)
                .get();
    }

    public static ContentValues buildTrackPoliciesValues(long date) {
        return ContentValuesBuilder.values()
                .put(TableColumns.TrackPolicies.LAST_UPDATED, date)
                .get();
    }
}
