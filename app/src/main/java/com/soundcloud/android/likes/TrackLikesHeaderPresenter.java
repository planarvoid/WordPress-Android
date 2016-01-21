package com.soundcloud.android.likes;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

public class TrackLikesHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment> {

    private final TrackLikesHeaderView headerView;
    private final OfflineContentOperations offlineContentOperations;
    private final OfflinePlaybackOperations playbackOperations;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final FeatureOperations featureOperations;
    private final EventBus eventBus;

    private final Action0 sendShuffleLikesAnalytics = new Action0() {
        @Override
        public void call() {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffleMyLikes());
        }
    };

    private final View.OnClickListener onShuffleButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            playbackOperations
                    .playLikedTracksShuffled(new PlaySessionSource(Screen.LIKES))
                    .doOnCompleted(sendShuffleLikesAnalytics)
                    .subscribe(expandPlayerSubscriberProvider.get());
        }
    };

    private Subscription foregroundSubscription = RxUtils.invalidSubscription();

    @Inject
    public TrackLikesHeaderPresenter(TrackLikesHeaderView headerView,
                                     OfflineContentOperations offlineContentOperations,
                                     OfflinePlaybackOperations playbackOperations,
                                     Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                     FeatureOperations featureOperations,
                                     EventBus eventBus) {
        this.headerView = headerView;
        this.offlineContentOperations = offlineContentOperations;
        this.playbackOperations = playbackOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.featureOperations = featureOperations;
        this.eventBus = eventBus;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        headerView.onViewCreated(view);
        headerView.setOnShuffleButtonClick(onShuffleButtonClick);
    }

    @Override
    public void onResume(Fragment fragment) {
        if (featureOperations.isOfflineContentOrUpsellEnabled()) {
            if (featureOperations.isOfflineContentEnabled()) {
                subscribeForOfflineContentUpdates();
            }
        } else {
            headerView.show(OfflineState.NOT_OFFLINE);
        }
    }

    public void updateTrackCount(int size) {
        if (headerView.isViewCreated()) {
            headerView.updateTrackCount(size);
        }
    }

    private void subscribeForOfflineContentUpdates() {
        Observable<OfflineState> offlineLikesState =
                eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                        .filter(OfflineContentChangedEvent.HAS_LIKED_COLLECTION_CHANGE)
                        .map(OfflineContentChangedEvent.TO_OFFLINE_STATE);

        foregroundSubscription = offlineLikesState
                .startWith(offlineContentOperations.getLikedTracksOfflineStateFromStorage())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OfflineLikesSettingSubscriber());
    }

    @Override
    public void onPause(Fragment fragment) {
        foregroundSubscription.unsubscribe();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        headerView.onDestroyView();
    }

    private class OfflineLikesSettingSubscriber extends DefaultSubscriber<OfflineState> {
        @Override
        public void onNext(OfflineState offlineState) {
            if (featureOperations.isOfflineContentEnabled()) {
                headerView.show(offlineState);
            }
        }
    }

}

