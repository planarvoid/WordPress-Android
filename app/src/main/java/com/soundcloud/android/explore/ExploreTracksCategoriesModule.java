package com.soundcloud.android.explore;

import com.soundcloud.android.dagger.AndroidObservableFactory;
import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {ExploreTracksCategoriesFragment.class})
public class ExploreTracksCategoriesModule {

    @Provides
    AndroidObservableFactory provideFactory(ExploreTracksOperations exploreTracksOperations){
        return new AndroidObservableFactory(exploreTracksOperations.getCategories());
    }

}
