package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventContextMetadata.builder;
import static com.soundcloud.android.rx.RxUtils.IS_PRESENT;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.Observable.combineLatest;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func4;
import rx.subjects.BehaviorSubject;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

public class TrackLikesHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment>
        implements TrackLikesHeaderView.Listener {

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
    private final Provider<UpdateHeaderViewSubscriber> subscriberProvider;

    private final Func4<Integer, TrackLikesHeaderView, OfflineState, Boolean, HeaderViewUpdate> toHeaderViewUpdate =
            new Func4<Integer, TrackLikesHeaderView, OfflineState, Boolean, HeaderViewUpdate>() {
                @Override
                public HeaderViewUpdate call(Integer trackCount,
                                             TrackLikesHeaderView view,
                                             OfflineState offlineState,
                                             Boolean offlineLikesEnabled) {
                    return HeaderViewUpdate.create(view,
                                                   trackCount,
                                                   featureOperations.isOfflineContentEnabled(),
                                                   offlineLikesEnabled,
                                                   featureOperations.upsellOfflineContent(),
                                                   offlineState);
                }
            };
    ;

    private Fragment fragment;

    private final Action0 sendShuffleLikesAnalytics = new Action0() {
        @Override
        public void call() {
            eventBus.publish(EventQueue.TRACKING, UIEvent
                    .fromShuffle(builder().pageName(Screen.LIKES.get()).build()));
        }
    };

    private final BehaviorSubject<Integer> trackCountSubject;
    private final BehaviorSubject<Optional<View>> viewSubject;
    private Subscription subscription;

    @Inject
    public TrackLikesHeaderPresenter(final TrackLikesHeaderViewFactory headerViewFactory,
                                     OfflineContentOperations offlineContentOperations,
                                     OfflineStateOperations offlineStateOperations,
                                     TrackLikeOperations likeOperations,
                                     final FeatureOperations featureOperations,
                                     PlaybackInitiator playbackInitiator,
                                     Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                     Provider<OfflineLikesDialog> syncLikesDialogProvider,
                                     Navigator navigator,
                                     EventBus eventBus,
                                     Provider<UpdateHeaderViewSubscriber> subscriberProvider) {

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
        this.subscriberProvider = subscriberProvider;

        trackCountSubject = BehaviorSubject.create(Consts.NOT_SET);
        viewSubject = BehaviorSubject.create(Optional.<View>absent());
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);

        subscription = combineLatest(trackCountSubject,
                                     headerViewObservable(headerViewFactory),
                                     getOfflineStateObservable(),
                                     getOfflineLikesEnabledObservable(),
                                     toHeaderViewUpdate).subscribe(subscriberProvider.get());
    }

    @Override
    public void onDestroy(Fragment fragment) {
        subscription.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        this.fragment = fragment;
        viewSubject.onNext(Optional.of(view));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewSubject.onNext(Optional.<View>absent());
        this.fragment = null;
    }

    void updateTrackCount(int trackCount) {
        trackCountSubject.onNext(trackCount);
    }

    private Observable<Boolean> getOfflineLikesEnabledObservable() {
        if (featureOperations.isOfflineContentEnabled()) {
            return offlineContentOperations.getOfflineLikedTracksStatusChanges()
                                           .observeOn(AndroidSchedulers.mainThread());
        } else {
            return Observable.just(false);
        }
    }

    private Observable<OfflineState> getOfflineStateObservable() {
        if (featureOperations.isOfflineContentEnabled()) {
            return eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                           .filter(OfflineContentChangedEvent.HAS_LIKED_COLLECTION_CHANGE)
                           .map(OfflineContentChangedEvent.TO_OFFLINE_STATE)
                           .startWith(offlineStateOperations.loadLikedTracksOfflineState())
                           .observeOn(AndroidSchedulers.mainThread());
        } else {
            return Observable.just(OfflineState.NOT_OFFLINE);
        }

    }

    @Override
    public void onShuffle() {
        playbackInitiator.playTracksShuffled(likeOperations.likedTrackUrns(),
                                             new PlaySessionSource(Screen.LIKES),
                                             featureOperations.isOfflineContentEnabled())
                         .doOnCompleted(sendShuffleLikesAnalytics)
                         .subscribe(expandPlayerSubscriberProvider.get());
    }

    @Override
    public void onUpsell() {
        navigator.openUpgrade(fragment.getActivity());
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forLikesClick());
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

    @NonNull
    private Observable<TrackLikesHeaderView> headerViewObservable(final TrackLikesHeaderViewFactory headerViewFactory) {
        return viewSubject.filter(IS_PRESENT).map(new Func1<Optional<View>, TrackLikesHeaderView>() {
            @Override
            public TrackLikesHeaderView call(Optional<View> viewOptional) {
                return headerViewFactory.create(viewOptional.get(), TrackLikesHeaderPresenter.this);
            }
        });
    }


    static final class UpdateHeaderViewSubscriber extends DefaultSubscriber<HeaderViewUpdate> {

        private final OfflineSettingsOperations offlineSettings;
        private final NetworkConnectionHelper connectionHelper;
        private final EventBus eventBus;

        private Optional<HeaderViewUpdate> previousUpdate = Optional.absent();

        @Inject
        UpdateHeaderViewSubscriber(OfflineSettingsOperations offlineSettings,
                                   NetworkConnectionHelper connectionHelper,
                                   EventBus eventBus) {
            this.offlineSettings = offlineSettings;
            this.connectionHelper = connectionHelper;
            this.eventBus = eventBus;
        }

        @Override
        public void onNext(HeaderViewUpdate headerViewUpdate) {
            TrackLikesHeaderView headerView = headerViewUpdate.getView();

            final int trackCount = headerViewUpdate.getTrackCount();
            if (trackCount >= 0) {
                headerView.updateTrackCount(trackCount);
            }

            if (headerViewUpdate.isOfflineContentEnabled()) {
                headerView.setDownloadedButtonState(headerViewUpdate.isOfflineLikesEnabled());
                configureOfflineState(headerView, headerViewUpdate.getOfflineState());

            } else if (upsellOfflineContentChanged(headerViewUpdate)) {
                headerView.showUpsell();
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forLikesImpression());

            } else {
                headerView.show(OfflineState.NOT_OFFLINE);
            }

            previousUpdate = Optional.of(headerViewUpdate);
        }

        private boolean upsellOfflineContentChanged(HeaderViewUpdate headerViewUpdate) {
            if (previousUpdate.isPresent()) {
                return previousUpdate.get().upsellOfflineContent() != headerViewUpdate.upsellOfflineContent();
            } else {
                return headerViewUpdate.upsellOfflineContent();
            }
        }

        private void configureOfflineState(TrackLikesHeaderView headerView, OfflineState offlineState) {
            headerView.show(offlineState);
            if (offlineState == OfflineState.REQUESTED) {
                showConnectionWarningIfNecessary(headerView);
            }
        }

        private void showConnectionWarningIfNecessary(TrackLikesHeaderView headerView) {
            if (offlineSettings.isWifiOnlyEnabled() && !connectionHelper.isWifiConnected()) {
                headerView.showNoWifi();
            } else if (!connectionHelper.isNetworkConnected()) {
                headerView.showNoConnection();
            }
        }
    }


    @AutoValue
    static abstract class HeaderViewUpdate {

        public static HeaderViewUpdate create(TrackLikesHeaderView view,
                                              int trackCount,
                                              boolean offlineContentEnabled,
                                              boolean offlineLikesEnabled,
                                              boolean upsellOfflineContent,
                                              OfflineState offlineState) {
            return new AutoValue_TrackLikesHeaderPresenter_HeaderViewUpdate(view,
                                                                            trackCount,
                                                                            offlineContentEnabled,
                                                                            offlineLikesEnabled,
                                                                            upsellOfflineContent,
                                                                            offlineState);
        }

        public abstract TrackLikesHeaderView getView();

        public abstract int getTrackCount();

        public abstract boolean isOfflineContentEnabled();

        public abstract boolean isOfflineLikesEnabled();

        public abstract boolean upsellOfflineContent();

        public abstract OfflineState getOfflineState();
    }
}

