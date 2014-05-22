package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.ui.PlayerFragment;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {
        PlaybackService.class,
        PlayerActivity.class,
        PlayerFragment.class,
        PlaybackActionReceiver.class
})
public class PlaybackModule {

}
