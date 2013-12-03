package com.soundcloud.android.explore;

import com.soundcloud.android.dagger.AndroidObservableFactory;
import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {ExploreGenresFragment.class})
public class ExploreTracksCategoriesFragmentModule {

    @Provides
    AndroidObservableFactory provideObservableFactory(ExploreTracksOperations exploreTracksOperations) {
        return new AndroidObservableFactory(exploreTracksOperations.getCategories());
    }

}
