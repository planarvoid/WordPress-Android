package com.soundcloud.android.playback.service;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.PlayerDeviceCompatibility;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManager;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManagerCompat;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManagerImpl;
import com.soundcloud.android.properties.ApplicationProperties;

import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class, injects = {
        PlaybackService.class
})
public class PlaybackServiceModule {

    @Provides
    public MediaPlayerManager provideMediaPlayerManager(PlayerDeviceCompatibility playerDeviceCompatibility){
        // if we are always playing mediaplayer, we should use compat mode which always releases async
        if (playerDeviceCompatibility.shouldForceMediaPlayer()){
            return new MediaPlayerManagerCompat();
        } else {
            return new MediaPlayerManagerImpl();
        }
    }

}
