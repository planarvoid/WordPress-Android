package com.soundcloud.android.downgrade;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                GoOffboardingActivity.class,
                GoOffboardingFragment.class
        })
public class DowngradeModule {
}
