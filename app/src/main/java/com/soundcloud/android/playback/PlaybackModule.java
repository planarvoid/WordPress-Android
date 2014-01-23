package com.soundcloud.android.playback;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;

@Module(complete = false, library = true,
        injects = {PlaybackService.class, PlayerActivity.class},
        includes = {StorageModule.class, ApiModule.class}
)
public class PlaybackModule {
}
