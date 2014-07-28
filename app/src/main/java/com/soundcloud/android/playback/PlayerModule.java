package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.rx.ScSchedulers;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import javax.inject.Named;

@Module(addsTo = ApplicationModule.class, injects = {
        PlayerActivity.class,
        ActivitiesActivity.class,
        PlayerFragment.class,
        PlaybackActionReceiver.class,
        WaveformView.class
})
public class PlayerModule {

    @Provides @Named("GraphicsScheduler")
    public Scheduler provideGraphicsScheduler() {
        return ScSchedulers.GRAPHICS_SCHEDULER;
    }
}
