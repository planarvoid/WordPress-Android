package com.soundcloud.android.playback;

import com.soundcloud.android.playback.service.PlaybackService;
import dagger.Module;

@Module(complete = false,
        injects = {
                PlaybackService.class, PlayerActivity.class
        })
public class PlaybackModule {
}
