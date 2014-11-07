package com.soundcloud.android.stream;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class, injects = {SoundStreamFragment.class})
public class SoundStreamModule {

    @Provides
    public ISoundStreamStorage provideSoundStreamStorage(FeatureFlags featureFlags, DatabaseScheduler databaseScheduler){
        if (featureFlags.isEnabled(Feature.API_MOBILE_STREAM)){
            return new SoundStreamStorage(databaseScheduler);
        } else {
            return new LegacySoundStreamStorage(databaseScheduler);
        }
    }

}
