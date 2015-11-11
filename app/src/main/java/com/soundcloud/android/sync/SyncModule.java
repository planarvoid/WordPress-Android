package com.soundcloud.android.sync;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.activities.ActivitiesStorage;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.LegacyActivitiesStorage;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.sync.activities.ActivitiesNotifier;
import com.soundcloud.android.sync.entities.EntitySyncModule;
import com.soundcloud.android.sync.likes.LikesSyncModule;
import com.soundcloud.android.sync.posts.PostsSyncModule;
import com.soundcloud.android.sync.timeline.TimelineSyncStorage;
import dagger.Module;
import dagger.Provides;

import android.content.SharedPreferences;

import javax.inject.Named;
import javax.inject.Provider;

@Module(addsTo = ApplicationModule.class,
        injects = {ApiSyncService.class, SyncAdapterService.class},
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
    TimelineSyncStorage soundStreamSyncStorage(@Named(StorageModule.STREAM_SYNC) SharedPreferences preferences) {
        return new TimelineSyncStorage(preferences);
    }

    @Provides
    @Named(ACTIVITIES_SYNC_STORAGE)
    TimelineSyncStorage activitiesSyncStorage(@Named(StorageModule.ACTIVITIES_SYNC) SharedPreferences preferences) {
        return new TimelineSyncStorage(preferences);
    }

    @Provides
    ActivitiesNotifier activitiesNotifier(Provider<LegacyActivitiesStorage> legacyStorage,
                                          Provider<ActivitiesStorage> activitiesStorage,
                                          ContentStats contentStats,
                                          ImageOperations imageOperations,
                                          FeatureFlags featureFlags) {
        return new ActivitiesNotifier(
                featureFlags.isEnabled(Flag.ACTIVITIES_REFACTOR) ? activitiesStorage.get() : legacyStorage.get(),
                contentStats,
                imageOperations
        );
    }
}
