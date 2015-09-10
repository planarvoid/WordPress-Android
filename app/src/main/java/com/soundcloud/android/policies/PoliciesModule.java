package com.soundcloud.android.policies;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                DailyUpdateService.class,
        })
public class PoliciesModule {

}
