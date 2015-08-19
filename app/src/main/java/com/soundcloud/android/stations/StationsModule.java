package com.soundcloud.android.stations;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {StationsHomeFragment.class})
public class StationsModule {}
