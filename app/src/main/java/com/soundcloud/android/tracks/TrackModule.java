package com.soundcloud.android.tracks;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                TrackInfoFragment.class
        }
)
public class TrackModule {

}
