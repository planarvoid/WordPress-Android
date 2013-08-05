package com.soundcloud.android.service.sync;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.EnumSet;

enum SyncContent {

    MySoundStream (Content.ME_SOUND_STREAM, SyncConfig.ACTIVITY_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyActivities  (Content.ME_ACTIVITIES, SyncConfig.ACTIVITY_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MySounds    (Content.ME_SOUNDS,     SyncConfig.TRACK_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyPlaylists (Content.ME_PLAYLISTS,  SyncConfig.PLAYLIST_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyLikes     (Content.ME_LIKES,      SyncConfig.TRACK_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
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

    public static EnumSet<SyncContent> NON_ACTIVITIES = EnumSet.complementOf(EnumSet.of(MySoundStream, MyActivities));

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
        SyncStateManager stateManager = new SyncStateManager();

        for (SyncContent sc : SyncContent.values()) {
            if (resultData.containsKey(sc.content.uri.toString()) &&
                !resultData.getBoolean(sc.content.uri.toString())) {
                final int misses = stateManager.incrementSyncMiss(sc.content.uri);

                Log.d(SyncAdapterService.TAG, "Sync endpoint unchanged, " + sc.content.uri +
                        " incrementing misses to " + misses);
            }
        }
    }
}
