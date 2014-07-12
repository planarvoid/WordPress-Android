package com.soundcloud.android.sync;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.storage.provider.Content;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

enum SyncCleanups {
    SoundStreamCleanup    (Content.SOUND_STREAM_CLEANUP,     SyncConfig.DEFAULT_STREAM_ITEMS_TO_KEEP),
    ActivitiesCleanup (Content.ACTIVITIES_CLEANUP,  SyncConfig.DEFAULT_ACTIVITY_ITEMS_TO_KEEP),
    TracksCleanup (Content.PLAYABLE_CLEANUP,  -1),
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
    public static List<Uri> getCleanupsDueForSync(boolean manual) {
        SyncStateManager syncStateManager = new SyncStateManager(SoundCloudApplication.instance);

        List<Uri> urisToSync = new ArrayList<Uri>();
        for (SyncCleanups sc : SyncCleanups.values()) {
            final LocalCollection lc = syncStateManager.fromContent(sc.content);

            if (manual || sc.shouldSync(lc.syncMisses(), lc.last_sync_success)) {
                final Uri uri = sc.toKeep > -1 ? Uri.parse(sc.content.uri.toString() + "?limit="+sc.toKeep) : sc.content.uri;
                urisToSync.add(uri);
            }
        }
        return urisToSync;
    }
}
