package com.soundcloud.android.collection;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.playhistory.PlayHistoryActivity;
import com.soundcloud.android.collection.playhistory.PlayHistoryCollectionPresenter;
import com.soundcloud.android.collection.playhistory.PlayHistoryFragment;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.playlists.PlaylistsCollectionActivity;
import com.soundcloud.android.collection.playlists.PlaylistsCollectionFragment;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedActivity;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedFragment;
import com.soundcloud.android.configuration.experiments.PlayHistoryExperiment;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

import javax.inject.Provider;

@Module(addsTo = ApplicationModule.class,
        injects = {
                CollectionFragment.class,
                CollectionPreviewView.class,
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
                                                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                                       PlayHistoryOperations playHistoryOperations,
                                                       Resources resources,
                                                       EventBus eventBus) {
        if (playHistoryExperiment.isEnabled()) {
            return new PlayHistoryCollectionPresenter(
                    swipeRefreshAttacher, collectionOperations, collectionOptionsStorage,
                    adapter, playHistoryExperiment, resources, eventBus, expandPlayerSubscriberProvider,
                    playHistoryOperations);
        } else {
            return new CollectionPresenter(
                    swipeRefreshAttacher, collectionOperations, collectionOptionsStorage,
                    adapter, optionsPresenter, resources, eventBus);
        }
    }

}
