package com.soundcloud.android.service.sync;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

enum SyncContent {
    MySounds(Content.ME_TRACKS, SyncConfig.TRACK_STALE_TIME),
    MyFavorites(Content.ME_FAVORITES, SyncConfig.TRACK_STALE_TIME),
    MyFollowings(Content.ME_FOLLOWINGS, SyncConfig.USER_STALE_TIME),
    MyFollowers(Content.ME_FOLLOWERS, SyncConfig.USER_STALE_TIME);

    SyncContent(Content content, long syncDelay) {
        this.content = content;
        this.syncDelay = syncDelay;
        this.prefSyncEnabledKey = "sync"+name();
    }

    public final Content content;
    public final long syncDelay;
    public final String prefSyncEnabledKey;

    public static List<Uri> configureSyncExtras(Context c, List<Uri> urisToSync, boolean force){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        for (SyncContent sc : SyncContent.values()){
            if (sp.getBoolean(sc.prefSyncEnabledKey, true)) {
                final long lastUpdated = LocalCollection.getLastSync(sc.content.uri, c.getContentResolver());
                if (System.currentTimeMillis() - lastUpdated > sc.syncDelay || force){
                    urisToSync.add(sc.content.uri);
                }
            }
        }
        return urisToSync;
    }
}
