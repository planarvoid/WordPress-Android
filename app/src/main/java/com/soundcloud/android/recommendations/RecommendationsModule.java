package com.soundcloud.android.recommendations;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                RecommendationsFragment.class
        })
public class RecommendationsModule {
}
