package com.soundcloud.android.likes;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListHeaderPresenter;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class TrackLikesHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment> implements ListHeaderPresenter {

    private final TrackLikesHeaderView headerView;
    private final TrackLikeOperations likeOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final OfflinePlaybackOperations playbackOperations;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final FeatureOperations featureOperations;
    private final EventBus eventBus;
    private final LikesMenuPresenter likesMenuPresenter;

    private final Action0 sendShuffleLikesAnalytics = new Action0() {
        @Override
        public void call() {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffleMyLikes());
        }
    };

    private final Func1<Object, Observable<List<Urn>>> loadAllTrackUrns = new Func1<Object, Observable<List<Urn>>>() {
        @Override
        public Observable<List<Urn>> call(Object unused) {
            return likeOperations.likedTrackUrns();
        }
    };

    private final View.OnClickListener onShuffleButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            playbackOperations
                    .playLikedTracksShuffled(new PlaySessionSource(Screen.SIDE_MENU_LIKES))
                    .doOnCompleted(sendShuffleLikesAnalytics)
                    .subscribe(expandPlayerSubscriberProvider.get());
        }
    };

    private final View.OnClickListener onOverflowMenuClick = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            likesMenuPresenter.show(view);
        }
    };

    private CompositeSubscription viewLifeCycle;
    private Subscription foregroundSubscription = RxUtils.invalidSubscription();

    @Inject
    public TrackLikesHeaderPresenter(TrackLikesHeaderView headerView,
                                     TrackLikeOperations likeOperations,
                                     OfflineContentOperations offlineContentOperations,
                                     OfflinePlaybackOperations playbackOperations,
                                     Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                     FeatureOperations featureOperations,
                                     EventBus eventBus,
                                     LikesMenuPresenter likesMenuPresenter) {
        this.headerView = headerView;
        this.likeOperations = likeOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.playbackOperations = playbackOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.featureOperations = featureOperations;
        this.eventBus = eventBus;
        this.likesMenuPresenter = likesMenuPresenter;
    }

    @Override
    public void onViewCreated(View view, ListView listView) {
        headerView.onViewCreated(view);
        headerView.setOnShuffleButtonClick(onShuffleButtonClick);
        headerView.attachToList(listView);

        viewLifeCycle = new CompositeSubscription();
        viewLifeCycle.add(eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER)
                .flatMap(loadAllTrackUrns)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AllLikedTracksSubscriber()));
    }

    @Override
    public void onResume(Fragment fragment) {
        if (shouldShowOverflowMenu()) {
            if (featureOperations.isOfflineContentEnabled()) {
                subscribeForOfflineContentUpdates();
            }
            headerView.showOverflowMenuButton();
            headerView.setOnOverflowMenuClick(onOverflowMenuClick);
        } else {
            headerView.hideOverflowMenuButton();
            headerView.show(DownloadState.NO_OFFLINE);
        }
    }

    private void subscribeForOfflineContentUpdates() {
        foregroundSubscription = new CompositeSubscription(
                likesDownloadState()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new DownloadStateSubscriber()),
                offlineContentOperations
                        .getOfflineLikesSettingsStatus()
                        .subscribe(new OfflineLikesSettingSubscriber())
        );
    }

    private Observable<DownloadState> likesDownloadState() {
        final Observable<DownloadState> downloadStateFromCurrentDownload =
                eventBus.queue(EventQueue.CURRENT_DOWNLOAD)
                        .filter(CurrentDownloadEvent.FOR_LIKED_TRACKS_FILTER)
                        .map(CurrentDownloadEvent.TO_DOWNLOAD_STATE);
        return offlineContentOperations
                .getLikedTracksDownloadStateFromStorage()
                .concatWith(downloadStateFromCurrentDownload);
    }

    @Override
    public void onPause(Fragment fragment) {
        foregroundSubscription.unsubscribe();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        headerView.onDestroyView();
        viewLifeCycle.unsubscribe();
    }

    private boolean shouldShowOverflowMenu() {
        return featureOperations.isOfflineContentEnabled() || featureOperations.shouldShowUpsell();
    }

    public void onSubscribeListObservers(CollectionBinding<TrackItem> collectionBinding) {
        ConnectableObservable<List<Urn>> allLikedTrackUrns = collectionBinding.items()
                .first()
                .flatMap(loadAllTrackUrns)
                .observeOn(AndroidSchedulers.mainThread())
                .replay();
        viewLifeCycle.add(allLikedTrackUrns.subscribe(new AllLikedTracksSubscriber()));
        viewLifeCycle.add(allLikedTrackUrns.connect());
    }

    private class AllLikedTracksSubscriber extends DefaultSubscriber<List<Urn>> {
        @Override
        public void onNext(List<Urn> allLikedTracks) {
            headerView.updateTrackCount(allLikedTracks.size());
            headerView.updateOverflowMenuButton(!allLikedTracks.isEmpty() && shouldShowOverflowMenu());
        }
    }

    private class DownloadStateSubscriber extends DefaultSubscriber<DownloadState> {
        @Override
        public void onNext(DownloadState state) {
            if (featureOperations.isOfflineContentEnabled()
                    && (state == DownloadState.NO_OFFLINE || offlineContentOperations.isOfflineLikedTracksEnabled())) {
                headerView.show(state);
            }
        }
    }

    private class OfflineLikesSettingSubscriber extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean isEnabled) {
            if (!isEnabled) {
                headerView.show(DownloadState.NO_OFFLINE);
            }
        }
    }
}

