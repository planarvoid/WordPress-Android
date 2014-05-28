package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.LegacyPlayerController;
import com.soundcloud.android.playback.ui.PlayerController;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.preferences.AccountSettingsActivity;
import com.soundcloud.android.preferences.NotificationSettingsActivity;
import com.soundcloud.android.preferences.ScSettingsActivity;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class,
        injects = {
                MainActivity.class,
                NavigationFragment.class,
                NavigationDrawerFragment.class,
                EmailOptInDialogFragment.class,
                ScSettingsActivity.class,
                SettingsActivity.class,
                AccountSettingsActivity.class,
                NotificationSettingsActivity.class
        })
public class MainModule {

    @Provides
    PlayerController providePlayerController(FeatureFlags featureFlags, PlayQueueManager playQueueManager, EventBus eventBus) {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            return new SlidingPlayerController(playQueueManager, eventBus);
        } else {
            return new LegacyPlayerController();
        }
    }

}
