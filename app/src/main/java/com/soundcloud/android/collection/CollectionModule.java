package com.soundcloud.android.collection;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                CollectionFragment.class,
                CollectionPreviewView.class,
                OfflineOnboardingActivity.class,
                ConfirmRemoveOfflineDialogFragment.class
        }
)
public class CollectionModule {

}
