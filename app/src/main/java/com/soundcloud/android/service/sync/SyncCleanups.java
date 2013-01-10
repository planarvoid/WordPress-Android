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

enum SyncCleanups {
    SoundStreamCleanup    (Content.SOUND_STREAM_CLEANUP,     SyncConfig.DEFAULT_STREAM_ITEMS_TO_KEEP),
    ActivitiesCleanup (Content.ACTIVITIES_CLEANUP,  SyncConfig.DEFAULT_ACTIVITY_ITEMS_TO_KEEP),
    TracksCleanup (Content.TRACK_CLEANUP,  -1),
    UsersCleanup (Content.USERS_CLEANUP,  -1);

    SyncCleanups(Content content, int toKeep) {
        this.content = content;
        this.toKeep = toKeep;
    }

    public final Content content;
    public final int toKeep;

    public boolean shouldSync(int misses, long lastSync) {
        return System.currentTimeMillis() - lastSync >= SyncConfig.CLEANUP_DELAY;
    }

    /**
     * Returns a list of cleanups that should run, based on recent changes.
     *
     * @param manual manual sync {@link android.content.ContentResolver#SYNC_EXTRAS_MANUAL}
     */
    public static List<Uri> getCleanupsDueForSync(Context c, boolean manual) {
        List<Uri> urisToSync = new ArrayList<Uri>();
        for (SyncCleanups sc : SyncCleanups.values()) {
            final LocalCollection lc = LocalCollection.fromContent(sc.content, c.getContentResolver(), false);
            if (manual || lc == null || sc.shouldSync(lc.syncMisses(), lc.last_sync_success)) {
                final Uri uri = sc.toKeep > -1 ? Uri.parse(sc.content.uri.toString() + "?limit="+sc.toKeep) : sc.content.uri;
                urisToSync.add(uri);
            }
        }
        return urisToSync;
    }
}
