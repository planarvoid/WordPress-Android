package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                DiscoveryActivity.class,
                DiscoveryFragment.class,
                RecommendedTracksActivity.class,
                RecommendedTracksFragment.class,
                SearchResultsActivity.class
        })
public class DiscoveryModule {
}
