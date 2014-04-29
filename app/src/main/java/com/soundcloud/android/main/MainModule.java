package com.soundcloud.android.main;

import dagger.Module;

@Module(complete = false,
        injects = {
                MainActivity.class,
                NavigationFragment.class,
                NavigationDrawerFragment.class,
                EmailOptInDialogFragment.class
        })
public class MainModule {
}
