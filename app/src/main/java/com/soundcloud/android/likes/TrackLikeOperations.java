package com.soundcloud.android.likes;

import static com.google.common.collect.Iterables.getLast;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.rx.Pager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class TrackLikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;
    static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;

    private final LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    private final LikedTrackStorage likedTrackStorage;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;

    private final Action1<List<PropertySet>> requestTracksSyncAction = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (networkConnectionHelper.isWifiConnected() && !propertySets.isEmpty()) {
                syncInitiator.requestTracksSync(propertySets);
            }
        }
    };

    private final Func1<SyncResult, Observable<List<PropertySet>>> loadInitialLikedTracks = new Func1<SyncResult, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(SyncResult syncResult) {
            return likedTrackStorage.loadTrackLikes(PAGE_SIZE, INITIAL_TIMESTAMP);
        }
    };

    private final Func1<Urn, Observable<PropertySet>> loadLikedTrack = new Func1<Urn, Observable<PropertySet>>() {
        @Override
        public Observable<PropertySet> call(Urn urn) {
            return likedTrackStorage.loadTrackLike(urn);
        }
    };

    @Inject
    public TrackLikeOperations(LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand,
                               LikedTrackStorage likedTrackStorage,
                               SyncInitiator syncInitiator,
                               EventBus eventBus,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               NetworkConnectionHelper networkConnectionHelper) {
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
        this.likedTrackStorage = likedTrackStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    Observable<PropertySet> onTrackLiked(){
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_LIKED_FILTER)
                .map(EntityStateChangedEvent.TO_URN)
                .flatMap(loadLikedTrack);
    }

    Observable<Urn> onTrackUnliked() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_UNLIKED_FILTER)
                .map(EntityStateChangedEvent.TO_URN);
    }

    Observable<List<PropertySet>> likedTracks() {
        return likedTracks(INITIAL_TIMESTAMP);
    }

    private Observable<List<PropertySet>> likedTracks(long beforeTime) {
        return loadLikedTracksInternal(beforeTime)
                .lift(new OperatorSwitchOnEmptyList<>(updatedLikedTracks()));
    }

    private Observable<List<PropertySet>> loadLikedTracksInternal(long beforeTime) {
        return likedTrackStorage.loadTrackLikes(PAGE_SIZE, beforeTime)
                .doOnNext(requestTracksSyncAction)
                .subscribeOn(scheduler);
    }

    Observable<List<PropertySet>> updatedLikedTracks() {
        return syncInitiator
                .syncTrackLikes()
                .observeOn(scheduler)
                .flatMap(loadInitialLikedTracks)
                .subscribeOn(scheduler);
    }

    Pager.PagingFunction<List<PropertySet>> pagingFunction() {
        return new Pager.PagingFunction<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    return loadLikedTracksInternal(getLast(result).get(LikeProperty.CREATED_AT).getTime());
                }
            }
        };
    }

    public Observable<List<Urn>> likedTrackUrns() {
        return loadLikedTrackUrnsCommand.toObservable().subscribeOn(scheduler);
    }

}
