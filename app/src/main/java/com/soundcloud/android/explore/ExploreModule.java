package com.soundcloud.android.explore;

import com.soundcloud.android.main.MainActivity;
import dagger.Module;

@Module(complete = false, injects = MainActivity.class, includes = {
        ExploreTracksFragmentModule.class, ExploreTracksCategoriesFragmentModule.class
})
public class ExploreModule {
}
