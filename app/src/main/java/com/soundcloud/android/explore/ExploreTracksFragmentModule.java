package com.soundcloud.android.explore;

import com.soundcloud.android.collections.views.GridSpacer;
import dagger.Module;
import dagger.Provides;

@Module (complete = false, injects = {ExploreTracksFragment.class})
public class ExploreTracksFragmentModule {

    @Provides
    public ExploreTracksAdapter provideExploreTracksAdapter(GridSpacer gridSpacer){
        return new ExploreTracksAdapter(gridSpacer);
    }

    @Provides
    public GridSpacer provideGridSpacer(){
        return new GridSpacer();
    }

}
