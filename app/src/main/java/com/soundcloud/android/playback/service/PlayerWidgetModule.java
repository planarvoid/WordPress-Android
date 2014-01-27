package com.soundcloud.android.playback.service;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = PlayerWidgetController.class, includes = {
        StorageModule.class, ApiModule.class
})
public class PlayerWidgetModule {

    @Provides
    public PlayerAppWidgetProvider appWidgetProvider() {
        return PlayerAppWidgetProvider.getInstance();
    }

}
