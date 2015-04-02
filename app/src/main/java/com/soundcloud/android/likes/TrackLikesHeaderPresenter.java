package com.soundcloud.android.likes;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.ListHeaderPresenter;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
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

public class TrackLikesHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment> implements View.OnClickListener, ListHeaderPresenter {

    private final TrackLikesHeaderView headerView;
    private final TrackLikeOperations likeOperations;
    private final OfflinePlaybackOperations playbackOperations;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

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

    private CompositeSubscription viewLifeCycle;
    private Subscription foregroundSubscription = Subscriptions.empty();

    @Inject
    public TrackLikesHeaderPresenter(TrackLikesHeaderView headerView,
                                     TrackLikeOperations likeOperations,
                                     OfflinePlaybackOperations playbackOperations,
                                     Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                     EventBus eventBus) {
        this.headerView = headerView;
        this.likeOperations = likeOperations;
        this.playbackOperations = playbackOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void onViewCreated(View view, ListView listView) {
        headerView.onViewCreated(view);
        headerView.setOnShuffleButtonClick(this);
        headerView.attachToList(listView);

        viewLifeCycle = new CompositeSubscription(
                eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                        .filter(EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER)
                        .flatMap(loadAllTrackUrns)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new AllLikedTracksSubscriber()));
    }

    @Override
    public void onResume(Fragment fragment) {
        foregroundSubscription = eventBus
                .queue(EventQueue.CURRENT_DOWNLOAD)
                .filter(CurrentDownloadEvent.FOR_LIKED_TRACKS_FILTER)
                .subscribe(new DownloadStateSubscriber());
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

    @Override
    public void onClick(View view) {
        playbackOperations
                .playLikedTracksShuffled(new PlaySessionSource(Screen.SIDE_MENU_LIKES))
                .doOnCompleted(sendShuffleLikesAnalytics)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    public void onSubscribeListObservers(ListBinding<PropertySet, TrackItem> listBinding) {
        ConnectableObservable<List<Urn>> allLikedTrackUrns = listBinding.getSource()
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
        }
    }

    private class DownloadStateSubscriber extends DefaultSubscriber<CurrentDownloadEvent> {
        @Override
        public void onNext(CurrentDownloadEvent event) {
            switch (event.kind) {
                case DOWNLOADED:
                    headerView.showDownloadedState();
                    break;
                case DOWNLOADING:
                    headerView.showDownloadingState();
                    break;
                case REQUESTED:
                    headerView.showDefaultState();
                    break;
                case NO_OFFLINE:
                    headerView.showDefaultState();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown state:" + event.kind);
            }
        }
    }

}

