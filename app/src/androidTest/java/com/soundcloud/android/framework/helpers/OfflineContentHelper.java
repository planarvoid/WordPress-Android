package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.Consts;
import com.soundcloud.android.backdoor.IntegrationTestsBroadcastReceiver;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.Date;

public class OfflineContentHelper {

    private static final File OFFLINE_DIR = new File(Consts.FILES_PATH, "offline");

    public static int offlineFilesCount() {
        return IOUtils.nullSafeListFiles(OFFLINE_DIR, null).length;
    }

    public static void clearOfflineContent(Context context) {
        // remove metadata - not sure how to do it differently
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        db.delete(Table.TrackDownloads.name(), null, null);
        db.delete(Table.OfflineContent.name(), null, null);
        // remove actual files
        IOUtils.cleanDir(OFFLINE_DIR);
    }

    public static void clearLikes(Context context) {
        final SQLiteDatabase db = DatabaseManager.getInstance(context).getWritableDatabase();
        Table.Likes.recreate(db);
    }

    public static void insertOfflinePlaylistAndTrackWithPolicy(Context context, Urn playlist, Urn track, Date date) {
        setOfflineTrack(context, track);
        setLastPolicyUpdate(context, date);
        addPlaylistWithTrack(context, playlist, track);
        setOfflinePlaylist(context, playlist);
    }

    private static void setOfflineTrack(Context context, Urn track) {
        final Intent intent = new Intent(IntegrationTestsBroadcastReceiver.Action.SET_TRACK_OFFLINE.name());
        intent.putExtra(IntegrationTestsBroadcastReceiver.TRACK, track);
        sendIntent(context, intent);
    }

    private static void setOfflinePlaylist(Context context, Urn playlist) {
        final Intent intent = new Intent(IntegrationTestsBroadcastReceiver.Action.SET_PLAYLIST_OFFLINE.name());
        intent.putExtra(IntegrationTestsBroadcastReceiver.PLAYLIST, playlist);
        sendIntent(context, intent);
    }

    private static void setLastPolicyUpdate(Context context, Date date) {
        final Intent intent = new Intent(IntegrationTestsBroadcastReceiver.Action.SET_LAST_POLICY_UPDATE.name());
        intent.putExtra(IntegrationTestsBroadcastReceiver.TIME, date.getTime());
        sendIntent(context, intent);
    }

    private static void addPlaylistWithTrack(Context context, Urn playlist, Urn track) {
        final Intent intent = new Intent(IntegrationTestsBroadcastReceiver.Action.ADD_LOCAL_PLAYLIST_WITH_TRACK.name());
        intent.putExtra(IntegrationTestsBroadcastReceiver.PLAYLIST, playlist);
        intent.putExtra(IntegrationTestsBroadcastReceiver.TRACK, track);
        sendIntent(context, intent);
    }

    private static void sendIntent(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
    }

}
