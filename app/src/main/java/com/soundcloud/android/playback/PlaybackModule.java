package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlayerWidgetController;
import com.soundcloud.android.playback.ui.PlayerFragment;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {
        PlaybackService.class, PlayerActivity.class, PlayerFragment.class, PlayerWidgetController.class
})
public class PlaybackModule {

}
