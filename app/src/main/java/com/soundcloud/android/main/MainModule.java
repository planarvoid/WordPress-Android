package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.playback.ui.LegacyPlayerController;
import com.soundcloud.android.playback.ui.PlayerController;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.preferences.AccountSettingsActivity;
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
                AccountSettingsActivity.class
        })
public class MainModule {

    @Provides
    PlayerController providePlayerController(FeatureFlags featureFlags, EventBus eventBus) {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            return new SlidingPlayerController(eventBus);
        } else {
            return new LegacyPlayerController();
        }
    }

}
