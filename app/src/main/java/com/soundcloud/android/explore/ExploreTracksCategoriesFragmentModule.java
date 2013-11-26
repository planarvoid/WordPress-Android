package com.soundcloud.android.explore;

import com.soundcloud.android.dagger.AndroidObservableFactory;
import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {ExploreTracksCategoriesFragment.class})
public class ExploreTracksCategoriesFragmentModule {

    @Provides
    AndroidObservableFactory provideObservableFactory(ExploreTracksOperations exploreTracksOperations) {
        return new AndroidObservableFactory(exploreTracksOperations.getCategories());
    }

    @Provides
    public ExploreTracksCategoriesAdapter provideCategoriesAdapter() {
        return new ExploreTracksCategoriesAdapter();
    }
}
