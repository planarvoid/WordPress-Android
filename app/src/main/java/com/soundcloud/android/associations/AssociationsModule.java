package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(
        addsTo = ApplicationModule.class,
        library = true
)
public class AssociationsModule {

    @Singleton
    @Provides
    FollowStatus provideFollowStatus() {
        return FollowStatus.get();
    }

}
