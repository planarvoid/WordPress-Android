package com.soundcloud.android.stations;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(
        addsTo = ApplicationModule.class,
        injects = {
                RecentStationsActivity.class,
                RecentStationsFragment.class
        }
)
public class StationsModule {
}
