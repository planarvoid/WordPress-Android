package com.soundcloud.android.main;

import dagger.Module;

@Module(complete = false,
        injects = {MainActivity.class, NavigationFragment.class, NavigationDrawerFragment.class})
public class MainModule {
}
