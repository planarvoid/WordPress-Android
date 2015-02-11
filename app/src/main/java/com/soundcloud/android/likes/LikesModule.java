package com.soundcloud.android.likes;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.actionbar.menu.ActionMenuController;
import com.soundcloud.android.actionbar.menu.DefaultActionMenuController;
import com.soundcloud.android.actionbar.menu.SyncActionMenuController;
import com.soundcloud.android.configuration.features.FeatureOperations;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(addsTo = ApplicationModule.class, injects = {TrackLikesFragment.class})
public class LikesModule {

    @Provides
    @Named("LikedTracks")
    ActionMenuController provideTrackLikesActionMenuController(
            Lazy<SyncActionMenuController> syncActionMenuController,
            Lazy<DefaultActionMenuController> defaultActionMenuControllerProvider,
            FeatureOperations featureOperations) {
        if (shouldShowSyncOptions(featureOperations)) {
            return syncActionMenuController.get();
        } else {
            return defaultActionMenuControllerProvider.get();
        }
    }

    private boolean shouldShowSyncOptions(FeatureOperations featureOperations) {
        return featureOperations.isOfflineContentEnabled() || featureOperations.isOfflineContentUpsellEnabled();
    }

}
