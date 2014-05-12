package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {LikesListFragment.class})
public class LikesModule {
}
