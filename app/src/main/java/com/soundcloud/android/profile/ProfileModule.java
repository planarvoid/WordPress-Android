package com.soundcloud.android.profile;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ProfileActivity.class,
                MeActivity.class
        })
public class ProfileModule { }
