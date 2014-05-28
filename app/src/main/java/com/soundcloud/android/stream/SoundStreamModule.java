package com.soundcloud.android.stream;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {SoundStreamFragment.class})
public class SoundStreamModule {
}
