package com.soundcloud.android.likes;

import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.android.Pager;
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
                               SyncInitiator syncInitiator,
                               EventBus eventBus,
                               @Named("Storage") Scheduler scheduler,
                               NetworkConnectionHelper networkConnectionHelper) {
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    public Observable<List<PropertySet>> likedTracks() {
        return likedTracks(Long.MAX_VALUE);
    }

    private Observable<List<PropertySet>> likedTracks(long beforeTime) {
        return loadLikedTracksCommand
                .with(new ChronologicalQueryParams(PAGE_SIZE, beforeTime))
                .toObservable()
                .doOnNext(requestTracksSyncAction)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedLikedTracks()));
    }

    public Observable<List<PropertySet>> updatedLikedTracks() {
        return syncInitiator.syncTrackLikes().map(toInitalPageParams).flatMap(loadLikedTracksCommand);
    }

    public Pager<List<PropertySet>> likedTracksPager() {
        return new Pager<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    return likedTracks(getLast(result).get(LikeProperty.CREATED_AT).getTime());
                }
            }
        };
    }

    public Observable<List<Urn>> likedTrackUrns() {
        return loadLikedTrackUrnsCommand.toObservable().subscribeOn(scheduler);
    }

}
