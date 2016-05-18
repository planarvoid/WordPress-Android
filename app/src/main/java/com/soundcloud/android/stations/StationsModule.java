package com.soundcloud.android.stations;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(
        addsTo = ApplicationModule.class,
        injects = {
                ShowAllStationsActivity.class,
                ShowAllStationsFragment.class
        }
)
public class StationsModule {}
