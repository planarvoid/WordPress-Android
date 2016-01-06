package com.soundcloud.android.collections;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                CollectionsFragment.class,
                CollectionPreviewView.class,
                OfflineOnboardingActivity.class
        }
)
public class CollectionsModule {

}
