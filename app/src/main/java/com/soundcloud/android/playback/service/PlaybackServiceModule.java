package com.soundcloud.android.playback.service;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {
        PlaybackService.class
})
public class PlaybackServiceModule {

}
