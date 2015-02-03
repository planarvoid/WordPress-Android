package com.soundcloud.android.likes;

import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
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
import java.util.Date;
import java.util.List;

public class LikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final LoadLikedTracksCommand loadLikedTracksCommand;
    private final LoadLikedPlaylistsCommand loadLikedPlaylistsCommand;
    private final LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    private final UpdateLikeCommand storeLikeCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;

    private final Action1<PropertySet> publishPlayableChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet changeSet) {
            final Urn urn = changeSet.get(PlayableProperty.URN);
            final int likeCount = changeSet.get(PlayableProperty.LIKES_COUNT);
            final boolean isLiked = changeSet.get(PlayableProperty.IS_LIKED);
            eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(urn, isLiked, likeCount));
        }
    };

    private final Func1<SyncResult, ChronologicalQueryParams> toInitalPageParams = new Func1<SyncResult, ChronologicalQueryParams>() {
        @Override
        public ChronologicalQueryParams call(SyncResult syncResult) {
            return new ChronologicalQueryParams(PAGE_SIZE, Long.MAX_VALUE);
        }
    };

    private final Pager<List<PropertySet>> likedTracksPager = new Pager<List<PropertySet>>() {
        @Override
        public Observable<List<PropertySet>> call(List<PropertySet> result) {
            if (result.size() < PAGE_SIZE) {
                return Pager.finish();
            } else {
                return likedTracks(getLast(result).get(LikeProperty.CREATED_AT).getTime());
            }
        }
    };

    private final Pager<List<PropertySet>> likedPlaylistsPager = new Pager<List<PropertySet>>() {
        @Override
        public Observable<List<PropertySet>> call(List<PropertySet> result) {
            if (result.size() < PAGE_SIZE) {
                return Pager.finish();
            } else {
                return likedPlaylists(getLast(result).get(LikeProperty.CREATED_AT).getTime());
            }
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


    private final Action1<List<PropertySet>> requestPlaylistsSyncAction = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (networkConnectionHelper.isWifiConnected() && !propertySets.isEmpty()) {
                syncInitiator.requestPlaylistSync(propertySets);
            }
        }
    };

    @Inject
    public LikeOperations(LoadLikedTracksCommand loadLikedTracksCommand,
                          LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand,
                          LoadLikedPlaylistsCommand loadLikedPlaylistsCommand,
                          UpdateLikeCommand storeLikeCommand,
                          SyncInitiator syncInitiator,
                          EventBus eventBus,
                          @Named("Storage") Scheduler scheduler,
                          NetworkConnectionHelper networkConnectionHelper) {
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.loadLikedPlaylistsCommand = loadLikedPlaylistsCommand;
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
        this.storeLikeCommand = storeLikeCommand;
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
                .flatMap(returnIfNonEmptyOr(updatedLikedTracks()));
    }

    public Observable<List<PropertySet>> updatedLikedTracks() {
        return syncInitiator.syncTrackLikes().map(toInitalPageParams).flatMap(loadLikedTracksCommand);
    }

    public Observable<List<PropertySet>> likedPlaylists() {
        return likedPlaylists(Long.MAX_VALUE);
    }

    public Observable<List<PropertySet>> likedPlaylists(long beforeTime) {
        return loadLikedPlaylistsCommand
                .with(new ChronologicalQueryParams(PAGE_SIZE, beforeTime))
                .toObservable()
                .doOnNext(requestPlaylistsSyncAction)
                .subscribeOn(scheduler)
                .flatMap(returnIfNonEmptyOr(updatedLikedPlaylists()));
    }

    public Observable<List<PropertySet>> updatedLikedPlaylists() {
        return syncInitiator.syncPlaylistLikes().map(toInitalPageParams).flatMap(loadLikedPlaylistsCommand);
    }

    public Observable<PropertySet> addLike(final PropertySet sound) {
        sound.put(PlayableProperty.IS_LIKED, true);
        Date likeTime = new Date();
        sound.put(LikeProperty.CREATED_AT, likeTime);
        sound.put(LikeProperty.ADDED_AT, likeTime);
        return toggleLike(sound);
    }

    public Observable<PropertySet> removeLike(final PropertySet sound) {
        sound.put(PlayableProperty.IS_LIKED, false);
        Date unlikeTime = new Date();
        sound.put(LikeProperty.CREATED_AT, unlikeTime);
        sound.put(LikeProperty.REMOVED_AT, unlikeTime);
        return toggleLike(sound);
    }

    private Observable<PropertySet> toggleLike(PropertySet likeProperties) {
        return storeLikeCommand
                .with(likeProperties)
                .toObservable()
                .doOnNext(publishPlayableChanged)
                .subscribeOn(scheduler);
    }

    private <CollT extends List> Func1<CollT, Observable<CollT>> returnIfNonEmptyOr(final Observable<CollT> syncAndLoadObservable) {
        return new Func1<CollT, Observable<CollT>>() {
            @Override
            public Observable<CollT> call(CollT result) {
                if (result.isEmpty()) {
                    return syncAndLoadObservable;
                } else {
                    return Observable.just(result);
                }
            }
        };
    }

    public Observable<List<Urn>> likedTrackUrns() {
        return loadLikedTrackUrnsCommand.toObservable().subscribeOn(scheduler);
    }

    public Pager<List<PropertySet>> likedTracksPager() {
        return likedTracksPager;
    }

    public Pager<List<PropertySet>> likedPlaylistsPager() {
        return likedPlaylistsPager;
    }
}
