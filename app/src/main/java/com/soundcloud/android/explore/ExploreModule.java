package com.soundcloud.android.explore;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.dagger.AndroidObservableFactory;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;
import dagger.Provides;

@Module(complete = false,
        injects = {ExploreFragment.class, ExploreTracksFragment.class, ExploreGenresFragment.class},
        includes = {StorageModule.class, ApiModule.class})
public class ExploreModule {
    @Provides
    AndroidObservableFactory provideObservableFactory(ExploreTracksOperations exploreTracksOperations) {
        return new AndroidObservableFactory(exploreTracksOperations.getCategories());
    }
}
