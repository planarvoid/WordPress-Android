package com.soundcloud.android.settings;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                SettingsActivity.class,
                SettingsFragment.class,
                OfflineSettingsActivity.class,
                NotificationSettingsFragment.class,
                OfflineSettingsFragment.class,
                ClearCacheDialog.class,
                NotificationSettingsActivity.class,
                LegalActivity.class,
                LicensesActivity.class
        })
public class SettingsModule {}
