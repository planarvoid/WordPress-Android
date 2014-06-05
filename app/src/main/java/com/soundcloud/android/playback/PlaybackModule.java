package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlayerWidgetController;
import com.soundcloud.android.playback.service.StreamPlaya;
import com.soundcloud.android.playback.service.managers.FroyoRemoteAudioManager;
import com.soundcloud.android.playback.service.managers.ICSRemoteAudioManager;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.properties.ApplicationProperties;
import dagger.Module;
import dagger.Provides;

import android.content.Context;
import android.os.Build;

import javax.inject.Inject;

@Module(addsTo = ApplicationModule.class, injects = {
        PlaybackService.class, PlayerActivity.class, PlayerFragment.class, PlayerWidgetController.class
})
public class PlaybackModule {

    @SuppressWarnings("unchecked")
    @Provides
    public IRemoteAudioManager provideRemoteAudioManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                return new ICSRemoteAudioManager(context);
            } catch (Exception e) {
                SoundCloudApplication.handleSilentException("Could not create remote audio manager", e);
            }
        }
        return new FroyoRemoteAudioManager(context);
    }

    @Provides
    public StreamPlaya.PlayerSwitcherInfo providePlayerSwitcherInfo(ApplicationProperties applicationProperties){
        if (applicationProperties.isReleaseBuild()){
            return new StreamPlaya.PlayerSwitcherInfo(9, 1);
        } else {
            return new StreamPlaya.PlayerSwitcherInfo(2, 2);
        }
    }
}
