package com.soundcloud.android.playback.service;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManager;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManagerCompat;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerManagerImpl;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class, injects = {
        PlaybackService.class
})
public class PlaybackServiceModule {

    @Provides
    public MediaPlayerManager provideMediaPlayerManager(){
        // if we are always playing mediaplayer, we should use compat mode which always releases async
        if (PlaybackConstants.FORCE_MEDIA_PLAYER){
            return new MediaPlayerManagerCompat();
        } else {
            return new MediaPlayerManagerImpl();
        }
    }

}
