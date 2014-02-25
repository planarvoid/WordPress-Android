package com.soundcloud.android.explore;

import dagger.Module;

@Module(complete = false,
        injects = {
        ExploreFragment.class,
        ExploreTracksFragment.class,
        ExploreGenresFragment.class
})
public class ExploreModule {
}
