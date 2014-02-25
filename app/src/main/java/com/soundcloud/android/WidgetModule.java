package com.soundcloud.android;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import com.soundcloud.android.playback.service.PlayerWidgetController;
import dagger.Module;
import dagger.Provides;

import android.content.Context;

import javax.inject.Singleton;

// Don't remove the injects on the widget controller, since it requires it to reach out for the singleton
// instance provided by this module!
@Module(addsTo = ApplicationModule.class, injects = PlayerWidgetController.class)
public class WidgetModule {

    @Provides
    @Singleton
    public PlayerWidgetController provideWidgetController(Context context, PlaybackStateProvider playbackStateProvider,
                                                          PlayerAppWidgetProvider widgetProvider,
                                                          SoundAssociationOperations soundAssociationOps, EventBus eventBus) {
        return new PlayerWidgetController(context, playbackStateProvider, widgetProvider, soundAssociationOps, eventBus);
    }

    @Provides
    @Singleton
    public PlayerAppWidgetProvider appWidgetProvider() {
        return new PlayerAppWidgetProvider();
    }
}
