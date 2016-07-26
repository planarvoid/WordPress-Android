package com.soundcloud.android.sync;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.sync.entities.EntitySyncModule;
import com.soundcloud.android.sync.likes.LikesSyncModule;
import com.soundcloud.android.sync.posts.PostsSyncModule;
import com.soundcloud.android.sync.timeline.TimelineSyncStorage;
import dagger.Module;
import dagger.Provides;

import android.content.SharedPreferences;

import javax.inject.Named;

@Module(complete = false,
        injects = {ApiSyncService.class, SyncAdapterService.class},
        library = true,
        includes = {
                LikesSyncModule.class,
                EntitySyncModule.class,
                PostsSyncModule.class
        })
public class SyncModule {

    public static final String SOUND_STREAM_SYNC_STORAGE = "SoundStreamSyncStorage";
    public static final String ACTIVITIES_SYNC_STORAGE = "ActivitiesSyncStorage";

    @Provides
    @Named(SOUND_STREAM_SYNC_STORAGE)
    public TimelineSyncStorage soundStreamSyncStorage(@Named(StorageModule.STREAM_SYNC) SharedPreferences preferences) {
        return new TimelineSyncStorage(preferences);
    }

    @Provides
    @Named(ACTIVITIES_SYNC_STORAGE)
    public TimelineSyncStorage activitiesSyncStorage(@Named(StorageModule.ACTIVITIES_SYNC) SharedPreferences preferences) {
        return new TimelineSyncStorage(preferences);
    }
}
