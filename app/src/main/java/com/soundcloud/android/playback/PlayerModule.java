package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.android.view.menu.PopupMenuWrapperCompat;
import com.soundcloud.android.view.menu.PopupMenuWrapperICS;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.rx.ScSchedulers;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import android.os.Build;

import javax.inject.Named;

@Module(addsTo = ApplicationModule.class, injects = {
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

    @Provides
    public PopupMenuWrapper.Factory providePopupMenuWrapperFactory() {
        if (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT){
            return new PopupMenuWrapperICS.Factory();
        } else {
            return new PopupMenuWrapperCompat.Factory();
        }
    }
}
