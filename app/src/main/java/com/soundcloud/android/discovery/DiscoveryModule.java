package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                DiscoveryFragment.class,
                RecommendedTracksActivity.class,
                RecommendedTracksFragment.class
        })
public class DiscoveryModule {
}
