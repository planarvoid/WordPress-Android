package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                MainActivity.class,
                NavigationFragment.class,
                NavigationDrawerFragment.class,
                EmailOptInDialogFragment.class
        })
public class MainModule {
}
