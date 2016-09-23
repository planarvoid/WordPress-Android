package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.playback.playqueue.PlayQueueFragment;
import com.soundcloud.android.playback.ui.BlurringPlayerArtworkLoader;
import com.soundcloud.android.playback.ui.PlayerArtworkLoader;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.view.GlassLinearLayout;
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
                PlayQueueFragment.class,
                GlassLinearLayout.class
        })
public class PlayerModule {

    @Provides
    public PlayerArtworkLoader providePlayerArtworkLoader(ImageOperations imageOperations, Resources resources,
                                                          @Named(ApplicationModule.LOW_PRIORITY) Scheduler graphicsScheduler) {
        // ScriptIntrinsicBlur is available in JB_MR1 but is very buggy
        if (Build.VERSION_CODES.JELLY_BEAN_MR1 < Build.VERSION.SDK_INT) {
            return new BlurringPlayerArtworkLoader(imageOperations, resources, graphicsScheduler);
        } else {
            return new PlayerArtworkLoader(imageOperations, resources);
        }
    }
}
