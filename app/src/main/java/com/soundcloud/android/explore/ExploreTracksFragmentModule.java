package com.soundcloud.android.explore;

import com.soundcloud.android.collections.views.GridSpacer;
import com.soundcloud.android.image.ImageOperations;
import dagger.Module;
import dagger.Provides;

@Module (complete = false, injects = {ExploreTracksFragment.class})
public class ExploreTracksFragmentModule {

    @Provides
    public ExploreTracksAdapter provideExploreTracksAdapter(GridSpacer gridSpacer, ImageOperations imageOperations){
        return new ExploreTracksAdapter(gridSpacer, imageOperations);
    }

    @Provides
    public GridSpacer provideGridSpacer(){
        return new GridSpacer();
    }

}
