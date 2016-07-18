package com.soundcloud.android.collection;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.playhistory.PlayHistoryActivity;
import com.soundcloud.android.collection.playhistory.PlayHistoryCollectionPresenter;
import com.soundcloud.android.collection.playhistory.PlayHistoryFragment;
import com.soundcloud.android.collection.playlists.PlaylistsCollectionActivity;
import com.soundcloud.android.collection.playlists.PlaylistsCollectionFragment;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedActivity;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedFragment;
import com.soundcloud.android.configuration.experiments.PlayHistoryExperiment;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

@Module(addsTo = ApplicationModule.class,
        injects = {
                CollectionFragment.class,
                CollectionPreviewView.class,
                GoOnboardingActivity.class,
                ConfirmRemoveOfflineDialogFragment.class,
                PlayHistoryActivity.class,
                PlayHistoryFragment.class,
                PlaylistsCollectionActivity.class,
                PlaylistsCollectionFragment.class,
                RecentlyPlayedActivity.class,
                RecentlyPlayedFragment.class
        }
)
public class CollectionModule {
    @Provides
    BaseCollectionPresenter provideCollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                                       CollectionOperations collectionOperations,
                                                       CollectionOptionsStorage collectionOptionsStorage,
                                                       CollectionAdapter adapter,
                                                       CollectionPlaylistOptionsPresenter optionsPresenter,
                                                       PlayHistoryExperiment playHistoryExperiment,
                                                       Resources resources,
                                                       EventBus eventBus,
                                                       FeatureFlags featureFlags) {
        if (featureFlags.isEnabled(Flag.LOCAL_PLAY_HISTORY)) {
            return new PlayHistoryCollectionPresenter(
                    swipeRefreshAttacher, collectionOperations, collectionOptionsStorage,
                    adapter, playHistoryExperiment, resources, eventBus);
        } else {
            return new CollectionPresenter(
                    swipeRefreshAttacher, collectionOperations, collectionOptionsStorage,
                    adapter, optionsPresenter, resources, eventBus);
        }
    }

}
