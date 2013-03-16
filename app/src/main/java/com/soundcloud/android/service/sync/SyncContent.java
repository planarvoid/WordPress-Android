package com.soundcloud.android.service.sync;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

enum SyncContent {
    MySounds    (Content.ME_SOUNDS,     SyncConfig.TRACK_STALE_TIME, SyncConfig.TRACK_BACKOFF_MULTIPLIERS),
    MyPlaylists (Content.ME_PLAYLISTS,  SyncConfig.PLAYLIST_STALE_TIME, SyncConfig.TRACK_BACKOFF_MULTIPLIERS),
    MyLikes     (Content.ME_LIKES,      SyncConfig.TRACK_STALE_TIME, SyncConfig.TRACK_BACKOFF_MULTIPLIERS),
    MyFollowings(Content.ME_FOLLOWINGS, SyncConfig.USER_STALE_TIME,  SyncConfig.USER_BACKOFF_MULTIPLIERS),
    MyFollowers (Content.ME_FOLLOWERS,  SyncConfig.USER_STALE_TIME,  SyncConfig.USER_BACKOFF_MULTIPLIERS),
    MyConnections (Content.ME_CONNECTIONS,  SyncConfig.CONNECTIONS_STALE_TIME,  null),
    MyFriends   (Content.ME_FRIENDS,  SyncConfig.USER_STALE_TIME,  SyncConfig.USER_BACKOFF_MULTIPLIERS),
    MyShortcuts (Content.ME_SHORTCUTS,  SyncConfig.SHORTCUTS_STALE_TIME,  null);

    SyncContent(Content content, long syncDelay, int[] backoffMultipliers) {
        this.content = content;
        this.syncDelay = syncDelay;
        this.prefSyncEnabledKey = "sync"+name();
        this.backoffMultipliers = backoffMultipliers;
    }

    public final Content content;
    public final long syncDelay;
    public final String prefSyncEnabledKey;
    public final int[] backoffMultipliers;

    public boolean isEnabled(SharedPreferences prefs) {
        return (this != MyFollowers) /* handled by push */
                && prefs.getBoolean(prefSyncEnabledKey, true);
    }

    public boolean setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.edit().putBoolean(prefSyncEnabledKey, enabled).commit();
    }

    public boolean shouldSync(int misses, long lastSync) {
        return backoffMultipliers == null || (misses < backoffMultipliers.length
            && System.currentTimeMillis() - lastSync >= syncDelay * backoffMultipliers[misses]);
    }


    public static void setAllSyncEnabledPrefs(Context c, boolean enabled) {
        for (SyncContent sc : SyncContent.values()) {
            sc.setEnabled(c, enabled);
        }
    }

    public static void updateCollections(Context c, Bundle resultData) {
        SyncStateManager stateManager = new SyncStateManager(c.getContentResolver());

        for (SyncContent sc : SyncContent.values()) {
            if (resultData.containsKey(sc.content.uri.toString()) &&
                !resultData.getBoolean(sc.content.uri.toString())) {
                final int misses = stateManager.incrementSyncMiss(sc.content.uri);

                if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) {
                    Log.d(SyncAdapterService.TAG, "Sync endpoint unchanged, " + sc.content.uri +
                            " incrementing misses to " + misses);
                }
            }
        }
    }

    public static @Nullable Set<Uri> getPlaylistsDueForSync(ContentResolver contentResolver) {
        Cursor c = contentResolver.query(Content.PLAYLIST_ALL_TRACKS.uri, new String[]{DBHelper.PlaylistTracks.PLAYLIST_ID},
                DBHelper.PlaylistTracks.ADDED_AT + " IS NOT NULL AND " + DBHelper.PlaylistTracks.PLAYLIST_ID + " > 0", null, null);

        if (c != null) {
            Set<Uri> uris = new HashSet<Uri>();
            while (c.moveToNext()) {
                uris.add(Content.PLAYLIST.forId(c.getLong(0)));
            }
            c.close();
            return uris;
        }

        return null;
    }
}
