package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import dagger.Module;
import dagger.Provides;

@Module(
        addsTo = ApplicationModule.class,
        library = true,
        injects = WhoToFollowActivity.class
)
public class AssociationsModule {

    @Provides
    FollowStatus provideFollowStatus() {
        return FollowStatus.get();
    }

    @Provides
    FollowingOperations provideFollowingOperations(RxHttpClient httpClient, UserAssociationStorage userAssociationStorage,
                                                         SyncStateManager syncStateManager, FollowStatus followStatus,
                                                         ScModelManager modelManager, SyncInitiator syncInitiator) {
        return new FollowingOperations(httpClient, userAssociationStorage, syncStateManager, followStatus, modelManager, syncInitiator);
    }
}
