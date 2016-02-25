package com.soundcloud.android.likes;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

public class TrackLikesHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment> implements TrackLikesHeaderView.Listener {

    private final TrackLikesHeaderViewFactory headerViewFactory;
    private final OfflineStateOperations offlineStateOperations;
    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final FeatureOperations featureOperations;
    private final EventBus eventBus;
    private final TrackLikeOperations likeOperations;
    private final Navigator navigator;
    private final Provider<OfflineLikesDialog> syncLikesDialogProvider;
    private final OfflineContentOperations offlineContentOperations;

    private Optional<TrackLikesHeaderView> viewOpt = Optional.absent();

    private Fragment fragment;

    private final Action0 sendShuffleLikesAnalytics = new Action0() {
        @Override
        public void call() {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffleMyLikes());
        }
    };

    private Subscription foregroundSubscription = RxUtils.invalidSubscription();

    @Inject
    public TrackLikesHeaderPresenter(TrackLikesHeaderViewFactory headerViewFactory,
                                     OfflineContentOperations offlineContentOperations,
                                     OfflineStateOperations offlineStateOperations,
                                     TrackLikeOperations likeOperations,
                                     FeatureOperations featureOperations,
                                     PlaybackInitiator playbackInitiator,
                                     Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                     Provider<OfflineLikesDialog> syncLikesDialogProvider,
                                     Navigator navigator,
                                     EventBus eventBus) {
        this.headerViewFactory = headerViewFactory;
        this.offlineStateOperations = offlineStateOperations;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.syncLikesDialogProvider = syncLikesDialogProvider;
        this.featureOperations = featureOperations;
        this.eventBus = eventBus;
        this.likeOperations = likeOperations;
        this.navigator = navigator;
        this.offlineContentOperations = offlineContentOperations;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        this.fragment = fragment;

        final TrackLikesHeaderView headerView = headerViewFactory.create(view, this);
        viewOpt = Optional.of(headerView);
    }

    @Override
    public void onResume(Fragment fragment) {
        if (viewOpt.isPresent()) {
            configureStates(viewOpt.get());
        }
    }

    private void configureStates(TrackLikesHeaderView headerView) {
        if (featureOperations.isOfflineContentEnabled()) {
            subscribeForOfflineContentUpdates();
        } else if (featureOperations.upsellOfflineContent()) {
            headerView.showUpsell();
            eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forLikesImpression());
        } else {
            headerView.show(OfflineState.NOT_OFFLINE);
        }
    }

    public void updateTrackCount(int size) {
        if (viewOpt.isPresent()) {
            viewOpt.get().updateTrackCount(size);
        }
    }

    private void subscribeForOfflineContentUpdates() {
        Observable<OfflineState> offlineLikesState =
                eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                        .filter(OfflineContentChangedEvent.HAS_LIKED_COLLECTION_CHANGE)
                        .map(OfflineContentChangedEvent.TO_OFFLINE_STATE);

        foregroundSubscription = new CompositeSubscription(
                offlineLikesState
                        .startWith(offlineStateOperations.loadLikedTracksOfflineState())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new OfflineLikesStateSubscriber()),
                offlineContentOperations.getOfflineLikedTracksStatusChanges()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new UpdateDownloadButtonSubscriber())
        );
    }

    @Override
    public void onPause(Fragment fragment) {
        foregroundSubscription.unsubscribe();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewOpt = Optional.absent();
        this.fragment = null;
    }

    private class OfflineLikesStateSubscriber extends DefaultSubscriber<OfflineState> {
        @Override
        public void onNext(OfflineState offlineState) {
            if (viewOpt.isPresent() && featureOperations.isOfflineContentEnabled()) {
                viewOpt.get().show(offlineState);
            }
        }
    }

    @Override
    public void onShuffle() {
        playbackInitiator.playTracksShuffled(likeOperations.likedTrackUrns(), new PlaySessionSource(Screen.LIKES))
                .doOnCompleted(sendShuffleLikesAnalytics)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    @Override
    public void onUpsell() {
        navigator.openUpgrade(fragment.getActivity());
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forLikesClick());
    }

    @Override
    public void onMakeAvailableOffline(boolean isAvailable) {
        if (isAvailable) {
            syncLikesDialogProvider.get().show(fragment.getFragmentManager());
        } else {
            disableOfflineLikes();
        }
    }

    private void disableOfflineLikes() {
        if (offlineContentOperations.isOfflineCollectionEnabled()) {
            ConfirmRemoveOfflineDialogFragment.showForLikes(fragment.getFragmentManager());
        } else {
            fireAndForget(offlineContentOperations.disableOfflineLikedTracks());
            eventBus.publish(EventQueue.TRACKING,
                    OfflineInteractionEvent.fromRemoveOfflineLikes(Screen.LIKES.get()));
        }
    }

    private final class UpdateDownloadButtonSubscriber extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean offlineLikesEnabled) {
            if (viewOpt.isPresent() && featureOperations.isOfflineContentEnabled()) {
                viewOpt.get().setDownloadedButtonState(offlineLikesEnabled);
            }
        }
    }

}

