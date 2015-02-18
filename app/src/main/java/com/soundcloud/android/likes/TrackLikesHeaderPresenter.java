package com.soundcloud.android.likes;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.lightcycle.DefaultFragmentLightCycle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.ListHeaderPresenter;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class TrackLikesHeaderPresenter extends DefaultFragmentLightCycle implements View.OnClickListener, ListHeaderPresenter {


    private final TrackLikesHeaderView headerView;
    private final LikeOperations likeOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final FeatureOperations featureOperations;
    private final PlaybackOperations playbackOperations;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

    private ConnectableObservable<List<Urn>> allLikedTrackUrns;
    private Subscription viewLifeCycle = Subscriptions.empty();
    private CompositeSubscription foregroundLifeCycle;

    private final Action0 sendShuffleLikesAnalytics = new Action0() {
        @Override
        public void call() {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffleMyLikes());
        }
    };

    private final Func1<List<PropertySet>, Observable<List<Urn>>> loadAllTrackUrns = new Func1<List<PropertySet>, Observable<List<Urn>>>() {
        @Override
        public Observable<List<Urn>> call(List<PropertySet> propertySets) {
            return likeOperations.likedTrackUrns();
        }
    };

    @Inject
    public TrackLikesHeaderPresenter(TrackLikesHeaderView headerView,
                                     LikeOperations likeOperations,
                                     OfflineContentOperations offlineContentOperations,
                                     FeatureOperations featureOperations, PlaybackOperations playbackOperations,
                                     Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                     EventBus eventBus) {
        this.headerView = headerView;
        this.likeOperations = likeOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.featureOperations = featureOperations;
        this.playbackOperations = playbackOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void onViewCreated(View view, ListView listView) {
        headerView.onViewCreated(view);
        headerView.setOnShuffleButtonClick(this);
        headerView.attachToList(listView);
    }

    @Override
    public void onResume(Fragment fragment) {
        foregroundLifeCycle = new CompositeSubscription();
        foregroundLifeCycle.add(offlineContentOperations.onStarted()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SyncStartedSubscriber()));

        foregroundLifeCycle.add(offlineContentOperations.onFinishedOrIdleWithDownloadedCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SyncFinishedOrIdleSubscriber()));
    }

    @Override
    public void onPause(Fragment fragment) {
        foregroundLifeCycle.unsubscribe();
    }

    public void onDestroyView() {
        headerView.onDestroyView();
        viewLifeCycle.unsubscribe();
    }

    @Override
    public void onClick(View view) {
        playbackOperations
                .playTracksShuffled(allLikedTrackUrns, new PlaySessionSource(Screen.SIDE_MENU_LIKES))
                .doOnCompleted(sendShuffleLikesAnalytics)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    public void onSubscribeListObservers(ListBinding<PropertySet, PropertySet> listBinding) {
        allLikedTrackUrns = listBinding.getSource()
                .first()
                .flatMap(loadAllTrackUrns)
                .observeOn(AndroidSchedulers.mainThread())
                .replay();
        allLikedTrackUrns.subscribe(new AllLikedTracksSubscriber());
        viewLifeCycle = allLikedTrackUrns.connect();
    }

    private class AllLikedTracksSubscriber extends DefaultSubscriber<List<Urn>> {
        @Override
        public void onNext(List<Urn> allLikedTracks) {
            headerView.updateTrackCount(allLikedTracks.size());
        }
    }

    private class SyncStartedSubscriber extends DefaultSubscriber<OfflineContentEvent> {
        @Override
        public void onNext(OfflineContentEvent unused) {
            if (isOfflineSyncEnabledAndAvailable()) {
                headerView.showSyncingState();
            } else {
                headerView.showDefaultState();
            }
        }
    }

    private class SyncFinishedOrIdleSubscriber extends DefaultSubscriber<Integer> {
        @Override
        public void onNext(Integer downloadedLikedTracksCount) {
            if (downloadedLikedTracksCount > 0 &&
                    isOfflineSyncEnabledAndAvailable()) {
                headerView.showDownloadedState();
            } else {
                headerView.showDefaultState();
            }
        }
    }

    private boolean isOfflineSyncEnabledAndAvailable() {
        return featureOperations.isOfflineContentEnabled() &&
                offlineContentOperations.isOfflineLikesEnabled();
    }

}
