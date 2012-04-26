package com.soundcloud.android.service.sync;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

enum SyncContent {
    MySounds    (Content.ME_TRACKS,     SyncConfig.TRACK_STALE_TIME, SyncConfig.TRACK_BACKOFF_MULTIPLIERS),
    MyFavorites (Content.ME_FAVORITES,  SyncConfig.TRACK_STALE_TIME, SyncConfig.TRACK_BACKOFF_MULTIPLIERS),
    MyFollowings(Content.ME_FOLLOWINGS, SyncConfig.USER_STALE_TIME,  SyncConfig.USER_BACKOFF_MULTIPLIERS),
    MyFollowers (Content.ME_FOLLOWERS,  SyncConfig.USER_STALE_TIME,  SyncConfig.USER_BACKOFF_MULTIPLIERS);

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
        return this != MyFollowers /* handled by push */ && prefs.getBoolean(prefSyncEnabledKey, true);
    }

    public boolean setEnabled(SharedPreferences prefs, boolean enabled) {
        return prefs.edit().putBoolean(prefSyncEnabledKey, enabled).commit();
    }

    public boolean shouldSync(int misses, long lastSync) {
        return misses < backoffMultipliers.length
            && System.currentTimeMillis() - lastSync >= syncDelay * backoffMultipliers[misses];
    }

    /**
     * Returns a list of uris to be synced, based on recent changes. The idea is that collections which don't change
     * very often don't get synced as frequently as collections which do.
     *
     * @param manual manual sync {@link android.content.ContentResolver.SYNC_EXTRAS_MANUAL}
     */
    public static List<Uri> getCollectionsDueForSync(Context c, boolean manual) {
        List<Uri> urisToSync = new ArrayList<Uri>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        for (SyncContent sc : SyncContent.values()) {
            if (sc.isEnabled(prefs)) {
                final LocalCollection lc = LocalCollection.fromContent(sc.content, c.getContentResolver(), false);
                if (manual || lc == null || sc.shouldSync(lc.syncMisses(), lc.last_sync)) {
                    urisToSync.add(sc.content.uri);
                }
            }
        }
        return urisToSync;
    }

    public static void setAllSyncEnabledPrefs(Context c, boolean enabled) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        for (SyncContent sc : SyncContent.values()) {
            sc.setEnabled(sp, enabled);
        }
    }

    public static void updateCollections(Context c, Bundle resultData) {
        for (SyncContent sc : SyncContent.values()) {
            if (resultData.containsKey(sc.content.uri.toString()) &&
                !resultData.getBoolean(sc.content.uri.toString())) {

                final int misses = LocalCollection.incrementSyncMiss(sc.content.uri, c.getContentResolver());

                if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) {
                    Log.d(SyncAdapterService.TAG, "Sync endpoint unchanged, " + sc.content.uri +
                            " incrementing misses to " + misses);
                }
            }
        }
    }
}
