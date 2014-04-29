package com.soundcloud.android.playback;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.managers.FroyoRemoteAudioManager;
import com.soundcloud.android.playback.service.managers.ICSRemoteAudioManager;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import dagger.Module;
import dagger.Provides;

import android.content.Context;
import android.os.Build;

@Module(complete = false, injects = {PlaybackService.class, PlayerActivity.class, SoundRecorder.class})
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
}
