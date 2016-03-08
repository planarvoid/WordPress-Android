package com.soundcloud.android.settings;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.settings.notifications.NotificationPreferencesActivity;
import com.soundcloud.android.settings.notifications.NotificationPreferencesFragment;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                SettingsActivity.class,
                OfflineSettingsActivity.class,
                NotificationSettingsFragment.class,
                NotificationPreferencesFragment.class,
                OfflineSettingsFragment.class,
                ClearCacheDialog.class,
                NotificationSettingsActivity.class,
                NotificationPreferencesActivity.class,
                LegalActivity.class,
                LicensesActivity.class
        })
public class SettingsModule {}
