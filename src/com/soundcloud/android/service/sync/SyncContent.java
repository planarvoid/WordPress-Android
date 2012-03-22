package com.soundcloud.android.service.sync;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

enum SyncContent {

    MySounds(Content.ME_TRACKS, SyncConfig.TRACK_STALE_TIME, SyncConfig.TRACK_BACKOFF_MULTIPLIERS),
    MyFavorites(Content.ME_FAVORITES, SyncConfig.TRACK_STALE_TIME, SyncConfig.TRACK_BACKOFF_MULTIPLIERS),
    MyFollowings(Content.ME_FOLLOWINGS, SyncConfig.USER_STALE_TIME, SyncConfig.USER_BACKOFF_MULTIPLIERS),
    MyFollowers(Content.ME_FOLLOWERS, SyncConfig.USER_STALE_TIME, SyncConfig.USER_BACKOFF_MULTIPLIERS);


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

    public static List<Uri> configureSyncExtras(Context c, List<Uri> urisToSync, boolean force){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        for (SyncContent sc : SyncContent.values()){
            if (sp.getBoolean(sc.prefSyncEnabledKey, true)) {
                final long lastUpdated = LocalCollection.getLastSync(sc.content.uri, c.getContentResolver());

                int syncMisses = 0;
                try {
                   syncMisses = Integer.parseInt(LocalCollection.getExtraFromUri(sc.content.uri, c.getContentResolver()));
                } catch (NumberFormatException ignore) {}

                if (force ||
                        // if too many misses, on demand sync only
                        (syncMisses < sc.backoffMultipliers.length
                        // make sure we are under the delay with exponential backoff factored in
                        && System.currentTimeMillis() - lastUpdated >= (sc.syncDelay * sc.backoffMultipliers[syncMisses]))
                    ){
                    urisToSync.add(sc.content.uri);
                }
            }
        }
        return urisToSync;
    }

    public static void setAllSyncEnabledPrefs(Context c, boolean enabled){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        for (SyncContent sc : SyncContent.values()){
            sp.edit().putBoolean(sc.prefSyncEnabledKey, enabled).commit();
        }
    }


    public static void updateCollections(Context c, Bundle resultData) {
        for (SyncContent sc : SyncContent.values()){
            if (resultData.containsKey(sc.content.uri.toString()) && !resultData.getBoolean(sc.content.uri.toString())){
                Log.i(SyncAdapterService.TAG,"Sync endpoint unchanged, " + sc.content.uri + " incrementing misses to "
                    + LocalCollection.incrementSyncMiss(sc.content.uri, c.getContentResolver()));
            }
        }
    }
}
