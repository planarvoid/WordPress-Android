package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.waveform.WaveformOperations;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {
        PlayerActivity.class,
        PlayerFragment.class,
        PlaybackActionReceiver.class,
})
public class PlayerModule {

}
