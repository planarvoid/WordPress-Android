package com.soundcloud.android.associations;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(
        complete = false, library = true
)
public class RepostsModule {

    @Singleton
    @Provides
    RepostCreator provideRepostCreator(LegacyRepostOperations legacyRepostOperations, RepostOperations repostOperations) {
        return legacyRepostOperations;
    }

}
