package com.soundcloud.android.collection;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                CollectionFragment.class,
                CollectionPreviewView.class,
                GoOnboardingActivity.class,
                ConfirmRemoveOfflineDialogFragment.class
        }
)
public class CollectionModule {

}
