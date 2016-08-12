package com.soundcloud.android.more;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                MoreFragment.class
        }
)
public class MoreModule {

}
