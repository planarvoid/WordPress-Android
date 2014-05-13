package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.preferences.ScSettingsActivity;
import com.soundcloud.android.preferences.SettingsActivity;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                MainActivity.class,
                NavigationFragment.class,
                NavigationDrawerFragment.class,
                EmailOptInDialogFragment.class,
                ScSettingsActivity.class,
                SettingsActivity.class
        })
public class MainModule {
}
