package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.playback.ui.BlurringPlayerArtworkLoader;
import com.soundcloud.android.playback.ui.PlayerArtworkLoader;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.rx.ScSchedulers;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import android.content.res.Resources;
import android.os.Build;

import javax.inject.Named;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ActivitiesActivity.class,
                PlayerFragment.class,
                PlaybackActionReceiver.class,
                WaveformView.class,

        })
public class PlayerModule {

    @Provides
    @Named("GraphicsScheduler")
    public Scheduler provideGraphicsScheduler() {
        return ScSchedulers.GRAPHICS_SCHEDULER;
    }

    @Provides
    public PlayerArtworkLoader providePlayerArtworkLoader(ImageOperations imageOperations, Resources resources,
                                                          @Named("GraphicsScheduler") Scheduler graphicsScheduler) {
        // ScriptIntrinsicBlur is available in JB_MR1 but is very buggy
        if (Build.VERSION_CODES.JELLY_BEAN_MR1 < Build.VERSION.SDK_INT) {
            return new BlurringPlayerArtworkLoader(imageOperations, resources, graphicsScheduler);
        } else {
            return new PlayerArtworkLoader(imageOperations, resources);
        }
    }
}
