package com.soundcloud.android.associations;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(
        complete = false, library = true
)
public class RepostsModule {

    @Singleton
    @Provides
    RepostCreator provideRepostCreator(FeatureFlags featureFlags, LegacyRepostOperations legacyRepostOperations, RepostOperations repostOperations) {
        if (featureFlags.isEnabled(Flag.NEW_POSTS_SYNCER)){
            return repostOperations;
        } else {
            return legacyRepostOperations;
        }
    }

}
