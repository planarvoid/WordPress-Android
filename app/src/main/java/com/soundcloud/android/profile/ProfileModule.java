package com.soundcloud.android.profile;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.associations.AssociationsModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ProfileActivity.class,
                MeActivity.class
        }, includes = AssociationsModule.class)
public class ProfileModule { }
