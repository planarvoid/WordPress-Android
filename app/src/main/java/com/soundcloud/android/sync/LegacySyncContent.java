package com.soundcloud.android.sync;

import com.soundcloud.android.storage.provider.Content;

import android.net.Uri;
import android.os.Bundle;

import java.util.EnumSet;

@Deprecated
public enum LegacySyncContent {

    MySoundStream (Content.ME_SOUND_STREAM, SyncConfig.ACTIVITY_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyActivities  (Content.ME_ACTIVITIES, SyncConfig.ACTIVITY_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),

    MySounds    (Content.ME_SOUNDS,     SyncConfig.TRACK_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyPlaylists (Content.ME_PLAYLISTS,  SyncConfig.PLAYLIST_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyLikes     (Content.ME_LIKES,      SyncConfig.TRACK_STALE_TIME, SyncConfig.DEFAULT_BACKOFF_MULTIPLIERS),
    MyFollowings(Content.ME_FOLLOWINGS, SyncConfig.USER_STALE_TIME,  SyncConfig.USER_BACKOFF_MULTIPLIERS);

    LegacySyncContent(Content content, long syncDelay, int[] backoffMultipliers) {
        this.content = content;
        this.syncDelay = syncDelay;
        this.backoffMultipliers = backoffMultipliers;
    }

    public static EnumSet<LegacySyncContent> NON_ACTIVITIES = EnumSet.complementOf(EnumSet.of(MySoundStream, MyActivities));

    public final Content content;
    public final long syncDelay;
    public final int[] backoffMultipliers;

    public boolean shouldSync(int misses, long lastSync) {
        if (backoffMultipliers == null){
            return true;
        } else {
            final int backoffIndex = Math.min(backoffMultipliers.length - 1, misses);
            return (System.currentTimeMillis() - lastSync >= syncDelay * backoffMultipliers[backoffIndex]);
        }
    }

    public static void updateCollections(SyncStateManager stateManager, Bundle resultData) {
        for (LegacySyncContent sc : LegacySyncContent.values()) {
            final String contentUri = sc.content.uri.toString();
            if (resultData.containsKey(contentUri)) {
                if (resultData.getBoolean(contentUri)) {
                    stateManager.resetSyncMisses(sc.content.uri);
                } else {
                    stateManager.incrementSyncMiss(sc.content.uri);
                }
            }
        }
    }

    // we won't be using Uris for syncing going forward, this just exists for legacy compat
    @Deprecated
    public Uri contentUri() {
        return content.uri;
    }
}
