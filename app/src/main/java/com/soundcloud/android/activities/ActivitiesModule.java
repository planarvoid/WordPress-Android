package com.soundcloud.android.activities;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.associations.AssociationsModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ActivitiesActivity.class,
                ActivitiesFragment.class
        }, includes = AssociationsModule.class)
public class ActivitiesModule { }
