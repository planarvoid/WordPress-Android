package com.soundcloud.android.likes;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {TrackLikesFragment.class})
public class LikesModule {}
