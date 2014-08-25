package com.soundcloud.android.playback.service;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManager;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManagerCompat;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManagerImpl;
import com.soundcloud.android.properties.ApplicationProperties;
import dagger.Module;
import dagger.Provides;

import android.os.Build;

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

    @Provides
    public MediaPlayerManager provideMediaPlayerManager(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD){
            return new MediaPlayerManagerImpl();
        } else {
            return new MediaPlayerManagerCompat();
        }
    }

}
