package com.soundcloud.android.likes;

import static com.google.common.collect.Iterables.getLast;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.android.NewPager;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class TrackLikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    private final LoadLikedTracksCommand loadLikedTracksCommand;
    private final LoadLikedTrackCommand loadLikedTrackCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;

    private final Func1<SyncResult, ChronologicalQueryParams> toInitalPageParams = new Func1<SyncResult, ChronologicalQueryParams>() {
        @Override
        public ChronologicalQueryParams call(SyncResult syncResult) {
            return new ChronologicalQueryParams(PAGE_SIZE, Long.MAX_VALUE);
        }
    };

    private final Action1<List<PropertySet>> requestTracksSyncAction = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (networkConnectionHelper.isWifiConnected() && !propertySets.isEmpty()) {
                syncInitiator.requestTracksSync(propertySets);
            }
        }
    };

    @Inject
    public TrackLikeOperations(LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand,
                               LoadLikedTracksCommand loadLikedTracksCommand,
                               LoadLikedTrackCommand loadLikedTrackCommand,
                               SyncInitiator syncInitiator,
                               EventBus eventBus,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               NetworkConnectionHelper networkConnectionHelper) {
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.loadLikedTrackCommand = loadLikedTrackCommand;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    public Observable<PropertySet> onTrackLiked(){
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_LIKED_FILTER)
                .map(EntityStateChangedEvent.TO_URN)
                .flatMap(loadLikedTrackCommand);
    }

    public Observable<Urn> onTrackUnliked() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_UNLIKED_FILTER)
                .map(EntityStateChangedEvent.TO_URN);
    }

    public Observable<List<PropertySet>> likedTracks() {
        return likedTracks(Long.MAX_VALUE);
    }

    private Observable<List<PropertySet>> likedTracks(long beforeTime) {
        return loadLikedTracksInternal(beforeTime)
                .lift(new OperatorSwitchOnEmptyList<>(updatedLikedTracks()));
    }

    private Observable<List<PropertySet>> loadLikedTracksInternal(long beforeTime) {
        return loadLikedTracksCommand
                .with(new ChronologicalQueryParams(PAGE_SIZE, beforeTime))
                .toObservable()
                .doOnNext(requestTracksSyncAction)
                .subscribeOn(scheduler);
    }

    public Observable<List<PropertySet>> updatedLikedTracks() {
        return syncInitiator
                .syncTrackLikes()
                .observeOn(scheduler)
                .map(toInitalPageParams)
                .flatMap(loadLikedTracksCommand)
                .subscribeOn(scheduler);
    }

    public NewPager.PagingFunction<List<PropertySet>> pagingFunction() {
        return new NewPager.PagingFunction<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return NewPager.finish();
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
