package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

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
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;

    private final Action1<List<PropertySet>> requestTracksSyncAction = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (networkConnectionHelper.isWifiConnected() && !propertySets.isEmpty()) {
                syncInitiator.batchSyncTracks(PropertySets.extractUrns(propertySets));
            }
        }
    };

    private final Func1<Object, Observable<List<PropertySet>>> loadInitialLikedTracks = new Func1<Object, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Object ignored) {
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
                               SyncInitiatorBridge syncInitiatorBridge,
                               EventBus eventBus,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               NetworkConnectionHelper networkConnectionHelper) {
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
        this.likedTrackStorage = likedTrackStorage;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    Observable<PropertySet> onTrackLiked() {
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
        return likedTracks(INITIAL_TIMESTAMP)
                .filter(RxUtils.IS_NOT_EMPTY_LIST)
                .switchIfEmpty(updatedLikedTracks());
    }

    Observable<List<PropertySet>> likedTracks(long beforeTime) {
        return likedTrackStorage.loadTrackLikes(PAGE_SIZE, beforeTime)
                                .doOnNext(requestTracksSyncAction)
                                .subscribeOn(scheduler);
    }

    Observable<List<PropertySet>> updatedLikedTracks() {
        return syncInitiatorBridge
                .syncTrackLikes()
                .observeOn(scheduler)
                .flatMap(loadInitialLikedTracks)
                .subscribeOn(scheduler);
    }

    public Observable<List<Urn>> likedTrackUrns() {
        return loadLikedTrackUrnsCommand.toObservable(null).subscribeOn(scheduler);
    }

}
