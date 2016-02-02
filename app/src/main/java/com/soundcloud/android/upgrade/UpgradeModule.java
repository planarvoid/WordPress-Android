package com.soundcloud.android.upgrade;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                OfflineOnboardingActivity.class,
        })
public class UpgradeModule {
}
