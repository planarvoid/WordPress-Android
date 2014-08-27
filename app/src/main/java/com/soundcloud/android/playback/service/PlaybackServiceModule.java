package com.soundcloud.android.playback.service;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.properties.ApplicationProperties;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class, injects = {
        PlaybackService.class
})
public class PlaybackServiceModule {

    @Provides
    public StreamPlaya.PlayerSwitcherInfo providePlayerSwitcherInfo(ApplicationProperties applicationProperties){
        if (applicationProperties.isReleaseBuild()){
            return new StreamPlaya.PlayerSwitcherInfo(9, 1);
        } else {
            return new StreamPlaya.PlayerSwitcherInfo(5, 1);
        }
    }
}
