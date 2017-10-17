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

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module(
    includes = {
        LikesSyncModule.class,
        EntitySyncModule.class,
        PostsSyncModule.class
    })
public abstract class SyncModule {

    public static final String SOUND_STREAM_SYNC_STORAGE = "SoundStreamSyncStorage";
    public static final String ACTIVITIES_SYNC_STORAGE = "ActivitiesSyncStorage";

    @Provides
    @Named(SOUND_STREAM_SYNC_STORAGE)
    static TimelineSyncStorage soundStreamSyncStorage(@Named(StorageModule.STREAM_SYNC) SharedPreferences preferences) {
        return new TimelineSyncStorage(preferences);
    }

    @Provides
    @Named(ACTIVITIES_SYNC_STORAGE)
    static TimelineSyncStorage activitiesSyncStorage(@Named(StorageModule.ACTIVITIES_SYNC) SharedPreferences preferences) {
        return new TimelineSyncStorage(preferences);
    }
}
