package com.soundcloud.android.tracks;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                TrackInfoActivity.class,
                TrackInfoFragment.class
        }
)
public class TrackModule {

}
