package com.soundcloud.android.likes;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.lightcycle.DefaultFragmentLightCycle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.HeaderViewPresenter;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class TrackLikesHeaderController extends DefaultFragmentLightCycle implements View.OnClickListener {

    private final TrackLikesHeaderPresenter headerPresenter;
    private final OfflineSyncEventOperations offlineContentEventsOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final FeatureOperations featureOperations;
    private final PlaybackOperations playbackOperations;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final EventBus eventBus;

    private CompositeSubscription subscription;
    private final List<Urn> likedTracks;


    private final Action0 sendShuffleLikesAnalytics = new Action0() {
        @Override
        public void call() {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffleMyLikes());
        }
    };

    @Inject
    public TrackLikesHeaderController(TrackLikesHeaderPresenter headerPresenter,
                                      OfflineSyncEventOperations offlineContentEventsOperations,
                                      OfflineContentOperations offlineContentOperations,
                                      FeatureOperations featureOperations, PlaybackOperations playbackOperations,
                                      Provider<ExpandPlayerSubscriber> subscriberProvider,
                                      EventBus eventBus) {
        this.headerPresenter = headerPresenter;
        this.offlineContentEventsOperations = offlineContentEventsOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.featureOperations = featureOperations;
        this.playbackOperations = playbackOperations;
        this.subscriberProvider = subscriberProvider;
        this.eventBus = eventBus;

        this.likedTracks = new ArrayList<>();
    }

    public HeaderViewPresenter getPresenter() {
        return headerPresenter;
    }

    public void setLikedTrackUrns(List<Urn> likedTracks) {
        this.likedTracks.clear();
        this.likedTracks.addAll(likedTracks);
        headerPresenter.updateTrackCount(likedTracks.size());
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        headerPresenter.onViewCreated(fragment, view, savedInstanceState);
        headerPresenter.setOnShuffleButtonClick(this);
    }

    @Override
    public void onResume(Fragment fragment) {
        subscription = new CompositeSubscription();
        subscription.add(offlineContentEventsOperations.onStarted()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SyncStartedSubscriber()));

        subscription.add(offlineContentEventsOperations.onFinishedOrIdleWithDownloadedCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SyncFinishedOrIdleSubscriber()));
    }

    @Override
    public void onPause(Fragment fragment) {
        subscription.unsubscribe();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        headerPresenter.onDestroyView();
    }

    private class SyncStartedSubscriber extends DefaultSubscriber<OfflineSyncEvent> {
        @Override
        public void onNext(OfflineSyncEvent offlineSyncEvent) {
            if (isOfflineSyncEnabledAndAvailable()) {
                headerPresenter.showSyncingState();
            } else {
                headerPresenter.showDefaultState(likedTracks.size());
            }
        }
    }

    private class SyncFinishedOrIdleSubscriber extends DefaultSubscriber<Integer> {
        @Override
        public void onNext(Integer downloadedLikedTracksCount) {
            if (downloadedLikedTracksCount > 0 &&
                    isOfflineSyncEnabledAndAvailable()) {
                headerPresenter.showDownloadedState(likedTracks.size());
            } else {
                headerPresenter.showDefaultState(likedTracks.size());
            }
        }
    }

    private boolean isOfflineSyncEnabledAndAvailable() {
        return featureOperations.isOfflineSyncEnabled() &&
                offlineContentOperations.isLikesOfflineSyncEnabled();
    }

    @Override
    public void onClick(View view) {
        playbackOperations
                .playTracksShuffled(likedTracks, new PlaySessionSource(Screen.SIDE_MENU_LIKES))
                .doOnCompleted(sendShuffleLikesAnalytics)
                .subscribe(subscriberProvider.get());
    }

}
