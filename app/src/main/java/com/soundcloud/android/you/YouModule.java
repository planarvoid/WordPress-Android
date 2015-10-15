package com.soundcloud.android.you;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                YouFragment.class
        }
)
public class YouModule {

}
