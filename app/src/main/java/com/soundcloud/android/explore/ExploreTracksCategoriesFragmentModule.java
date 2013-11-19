package com.soundcloud.android.explore;

import com.soundcloud.android.model.ExploreTracksCategories;
import dagger.Module;
import dagger.Provides;
import rx.Observable;

@Module(complete = false, injects = {ExploreTracksCategoriesFragment.class})
public class ExploreTracksCategoriesFragmentModule {
    @Provides
    Observable<ExploreTracksCategories> provideCategoryObservable() {
        return new ExploreTracksOperations().getCategories();
    }

}
