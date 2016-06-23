package com.soundcloud.android.playback.widget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {PlayerWidgetReceiver.class,
                PlayerAppWidgetProvider.class})
public class WidgetModule {
}
