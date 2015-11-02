package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                DiscoveryActivity.class,
                DiscoveryFragment.class,
                RecommendedTracksActivity.class,
                RecommendedTracksFragment.class,
                NewSearchActivity.class,
                SearchFragment.class,
                SearchResultsActivity.class,
                PlaylistDiscoveryActivity.class
        })
public class DiscoveryModule {
}
