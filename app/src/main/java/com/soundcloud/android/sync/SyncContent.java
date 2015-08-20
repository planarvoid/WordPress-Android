package com.soundcloud.android.sync;

import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.Log;

import android.os.Bundle;

import java.util.EnumSet;

public enum SyncContent {

    MySoundStream (Content.ME_SOUND_STREAM, SyncConfig.ACTIVITY_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyActivities  (Content.ME_ACTIVITIES, SyncConfig.ACTIVITY_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MySounds    (Content.ME_SOUNDS,     SyncConfig.TRACK_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyPlaylists (Content.ME_PLAYLISTS,  SyncConfig.PLAYLIST_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyLikes     (Content.ME_LIKES,      SyncConfig.TRACK_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyFollowings(Content.ME_FOLLOWINGS, SyncConfig.USER_STALE_TIME,  SyncConfig.USER_BACKOFF_MULTIPLIERS),
    MyFollowers (Content.ME_FOLLOWERS,  SyncConfig.USER_STALE_TIME,  SyncConfig.USER_BACKOFF_MULTIPLIERS),
    MyShortcuts (Content.ME_SHORTCUTS,  SyncConfig.SHORTCUTS_STALE_TIME,  null);

    SyncContent(Content content, long syncDelay, int[] backoffMultipliers) {
        this.content = content;
        this.syncDelay = syncDelay;
        this.backoffMultipliers = backoffMultipliers;
    }

    public static EnumSet<SyncContent> NON_ACTIVITIES = EnumSet.complementOf(EnumSet.of(MySoundStream, MyActivities));

    public final Content content;
    public final long syncDelay;
    public final int[] backoffMultipliers;

    public boolean isEnabled() {
        return this != MyFollowers; // Handled by push
    }

    public boolean shouldSync(int misses, long lastSync) {
        if (backoffMultipliers == null){
            return true;
        } else {
            final int backoffIndex = Math.min(backoffMultipliers.length - 1, misses);
            return (System.currentTimeMillis() - lastSync >= syncDelay * backoffMultipliers[backoffIndex]);
        }
    }

    public static void updateCollections(SyncStateManager stateManager, Bundle resultData) {
        for (SyncContent sc : SyncContent.values()) {
            final String contentUri = sc.content.uri.toString();
            if (resultData.containsKey(contentUri)) {
                if (resultData.getBoolean(contentUri)) {
                    stateManager.resetSyncMisses(sc.content.uri);
                    Log.d(SyncAdapterService.TAG, "Sync endpoint changed, reset misses for " + sc.content.uri);
                } else {
                    final int misses = stateManager.incrementSyncMiss(sc.content.uri);
                    Log.d(SyncAdapterService.TAG, "Sync endpoint unchanged, " + sc.content.uri +
                            " incremented misses to " + misses);
                }
            }
        }
    }
}
