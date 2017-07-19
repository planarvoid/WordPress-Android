package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventContextMetadata.builder;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.settings.OfflineStorageErrorDialog;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function4;
import io.reactivex.subjects.BehaviorSubject;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;

@SuppressWarnings("PMD.GodClass")
public class TrackLikesHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment>
        implements TrackLikesHeaderView.Listener, CellRenderer<TrackLikesItem> {

    private static final Function<Optional<WeakReference<View>>, Optional<View>> EXTRACT_VIEW = weakReferenceOptional -> weakReferenceOptional.transform(Reference::get);

    private final TrackLikesHeaderViewFactory headerViewFactory;
    private final OfflineStateOperations offlineStateOperations;
    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSingleObserver> expandPlayerObserverProvider;
    private final FeatureOperations featureOperations;
    private final EventBusV2 eventBus;
    private final TrackLikeOperations likeOperations;
    private final NavigationExecutor navigationExecutor;
    private final Provider<OfflineLikesDialog> syncLikesDialogProvider;
    private final OfflineContentOperations offlineContentOperations;
    private final Provider<UpdateHeaderViewObserver> observerProvider;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;

    private final Function4<Integer, TrackLikesHeaderView, OfflineState, Boolean, HeaderViewUpdate> toHeaderViewUpdate =
            new Function4<Integer, TrackLikesHeaderView, OfflineState, Boolean, HeaderViewUpdate>() {
                @Override
                public HeaderViewUpdate apply(Integer trackCount,
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

    private final Function<View, TrackLikesHeaderView> toTrackLikesHeaderView = new Function<View, TrackLikesHeaderView>() {
        @Override
        public TrackLikesHeaderView apply(View view) {
            return headerViewFactory.create(view, TrackLikesHeaderPresenter.this);
        }

    };

    private Fragment fragment;

    private final BehaviorSubject<Integer> trackCountSubject;
    private final BehaviorSubject<Optional<WeakReference<View>>> viewSubject;
    private final CompositeDisposable compositeDisposables = new CompositeDisposable();

    @Inject
    public TrackLikesHeaderPresenter(final TrackLikesHeaderViewFactory headerViewFactory,
                                     OfflineContentOperations offlineContentOperations,
                                     OfflineStateOperations offlineStateOperations,
                                     TrackLikeOperations likeOperations,
                                     final FeatureOperations featureOperations,
                                     PlaybackInitiator playbackInitiator,
                                     Provider<ExpandPlayerSingleObserver> expandPlayerObserverProvider,
                                     Provider<OfflineLikesDialog> syncLikesDialogProvider,
                                     NavigationExecutor navigationExecutor,
                                     EventBusV2 eventBus,
                                     Provider<UpdateHeaderViewObserver> observerProvider,
                                     OfflineSettingsStorage offlineSettingsStorage,
                                     GoOnboardingTooltipExperiment goOnboardingTooltipExperiment) {
        this.headerViewFactory = headerViewFactory;
        this.offlineStateOperations = offlineStateOperations;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerObserverProvider = expandPlayerObserverProvider;
        this.syncLikesDialogProvider = syncLikesDialogProvider;
        this.featureOperations = featureOperations;
        this.eventBus = eventBus;
        this.likeOperations = likeOperations;
        this.navigationExecutor = navigationExecutor;
        this.offlineContentOperations = offlineContentOperations;
        this.observerProvider = observerProvider;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.goOnboardingTooltipExperiment = goOnboardingTooltipExperiment;

        trackCountSubject = BehaviorSubject.createDefault(Consts.NOT_SET);
        viewSubject = BehaviorSubject.createDefault(Optional.<WeakReference<View>>absent());
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);

        compositeDisposables.add(Observable.combineLatest(trackCountSubject,
                                                          headerViewObservable(),
                                                          getOfflineStateObservable(),
                                                          getOfflineLikesEnabledObservable(),
                                                          toHeaderViewUpdate)
                                           .subscribeWith(observerProvider.get()));
    }

    @Override
    public void onDestroy(Fragment fragment) {
        compositeDisposables.clear();
        super.onDestroy(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        this.fragment = fragment;
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        this.fragment = null;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.track_likes_header, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackLikesItem> items) {
        viewSubject.onNext(Optional.of(new WeakReference<>(itemView)));
    }

    void updateTrackCount(int trackCount) {
        trackCountSubject.onNext(trackCount);
    }

    private Observable<Boolean> getOfflineLikesEnabledObservable() {
        if (featureOperations.isOfflineContentEnabled()) {
            return RxJava.toV2Observable(offlineContentOperations.getOfflineLikedTracksStatusChanges())
                         .observeOn(AndroidSchedulers.mainThread());
        } else {
            return Observable.just(false);
        }
    }

    private Observable<OfflineState> getOfflineStateObservable() {
        if (featureOperations.isOfflineContentEnabled()) {
            return eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                           .filter(event -> event.isLikedTrackCollection)
                           .map(event -> event.state)
                           .startWith(offlineStateOperations.loadLikedTracksOfflineState().toObservable())
                           .observeOn(AndroidSchedulers.mainThread());
        } else {
            return Observable.just(OfflineState.NOT_OFFLINE);
        }

    }

    @Override
    public void onShuffle() {
        compositeDisposables.add(playbackInitiator.playTracksShuffled(likeOperations.likedTrackUrns(), new PlaySessionSource(Screen.LIKES))
                                                  .doOnEvent((a, b) -> eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffle(builder().pageName(Screen.LIKES.get())
                                                                                                                                            .build())))
                                                  .subscribeWith(expandPlayerObserverProvider.get()));
    }

    @Override
    public void onUpsell() {
        navigationExecutor.openUpgrade(fragment.getActivity(), UpsellContext.OFFLINE);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forLikesClick());
    }

    @Override
    public void onMakeAvailableOffline(boolean isAvailable) {
        if (isAvailable) {
            enableOfflineLikes();
        } else {
            disableOfflineLikes();
        }
    }

    private void enableOfflineLikes() {
        if (offlineSettingsStorage.isOfflineContentAccessible()) {
            handleEnableOfflineLikes();
        } else {
            OfflineStorageErrorDialog.show(fragment.getFragmentManager());
        }
    }

    private void handleEnableOfflineLikes() {
        if (goOnboardingTooltipExperiment.isEnabled()) {
            fireAndForget(offlineContentOperations.enableOfflineLikedTracks());
        } else {
            syncLikesDialogProvider.get().show(fragment.getFragmentManager());
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
    private Observable<TrackLikesHeaderView> headerViewObservable() {
        return viewSubject.map(EXTRACT_VIEW)
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .map(toTrackLikesHeaderView);
    }

    static final class UpdateHeaderViewObserver extends DefaultObserver<HeaderViewUpdate> {

        private final OfflineSettingsOperations offlineSettings;
        private final ConnectionHelper connectionHelper;
        private final EventBusV2 eventBus;
        private final GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;

        private Optional<HeaderViewUpdate> previousUpdate = Optional.absent();

        @Inject
        UpdateHeaderViewObserver(OfflineSettingsOperations offlineSettings,
                                   ConnectionHelper connectionHelper,
                                   EventBusV2 eventBus,
                                   GoOnboardingTooltipExperiment goOnboardingTooltipExperiment) {
            this.offlineSettings = offlineSettings;
            this.connectionHelper = connectionHelper;
            this.eventBus = eventBus;
            this.goOnboardingTooltipExperiment = goOnboardingTooltipExperiment;
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
                showOfflineIntroductoryOverlayIfNeeded(headerView, headerViewUpdate.isOfflineLikesEnabled());

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

        private void showOfflineIntroductoryOverlayIfNeeded(TrackLikesHeaderView headerView, boolean offlineLikesEnabled) {
            if (shouldShowOfflineIntroductoryOverlay(offlineLikesEnabled) && canSyncOfflineOnCurrentConnection()) {
                headerView.showOfflineIntroductoryOverlay();
            }
        }

        private boolean shouldShowOfflineIntroductoryOverlay(boolean offlineLikesEnabled) {
            return !offlineLikesEnabled && goOnboardingTooltipExperiment.isEnabled();
        }

        private boolean canSyncOfflineOnCurrentConnection() {
            return offlineSettings.isWifiOnlyEnabled()
                   ? connectionHelper.isWifiConnected()
                   : connectionHelper.isNetworkConnected();
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

