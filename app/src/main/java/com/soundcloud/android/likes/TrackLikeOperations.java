package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.utils.RepoUtils.enrich;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.likes.LoadLikedTracksCommand.Params;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func2;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class TrackLikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;
    static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;
    private static final Func2<TrackItem, Like, LikeWithTrack> COMBINER = (trackItem, like) -> LikeWithTrack
            .create(like, trackItem);

    private final LoadLikedTracksCommand loadLikedTracksCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final EventBus eventBus;
    private final ConnectionHelper connectionHelper;
    private final TrackItemRepository trackRepo;

    private final Action1<List<Like>> requestTracksSyncAction = new Action1<List<Like>>() {
        @Override
        public void call(List<Like> likes) {
            if (connectionHelper.isWifiConnected() && !likes.isEmpty()) {
                syncInitiator.batchSyncTracks(transform(likes, Like::urn));
            }
        }
    };

    @Inject
    public TrackLikeOperations(LoadLikedTracksCommand loadLikedTracksCommand,
                               SyncInitiator syncInitiator,
                               SyncInitiatorBridge syncInitiatorBridge,
                               EventBus eventBus,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               ConnectionHelper connectionHelper,
                               TrackItemRepository trackItemRepository) {
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
        this.connectionHelper = connectionHelper;
        this.trackRepo = trackItemRepository;
    }

    Observable<TrackItem> onTrackLiked() {
        return singleTrackLikeStatusChange(true).flatMap(trackRepo::track);
    }

    Observable<Urn> onTrackUnliked() {
        return singleTrackLikeStatusChange(false);
    }

    private Observable<Urn> singleTrackLikeStatusChange(boolean wasLiked) {
        return eventBus.queue(LIKE_CHANGED)
                       .filter(event -> event.likes().keySet().size() == 1)
                       .map(event -> event.likes().values().iterator().next())
                       .filter(like -> like.urn().isTrack() && like.isUserLike() == wasLiked)
                       .map(LikesStatusEvent.LikeStatus::urn);
    }

    Observable<List<LikeWithTrack>> likedTracks() {
        return RxJava.toV1Observable(syncInitiatorBridge
                .hasSyncedTrackLikesBefore())
                .flatMap(hasSynced -> {
                    if (hasSynced) {
                        return likedTracks(INITIAL_TIMESTAMP);
                    } else {
                        return updatedLikedTracks();
                    }
                }).subscribeOn(scheduler);
    }

    Observable<List<LikeWithTrack>> likedTracks(long beforeTime) {
        Params params = Params.from(beforeTime, PAGE_SIZE);
        return loadLikedTracksCommand.toObservable(Optional.of(params))
                                     .doOnNext(requestTracksSyncAction)
                                     .flatMap(source -> enrich(source, trackRepo.fromUrns(transform(source, UrnHolder::urn)), COMBINER))
                                     .subscribeOn(scheduler);
    }

    Observable<List<LikeWithTrack>> updatedLikedTracks() {
        return syncInitiatorBridge
                .syncTrackLikes()
                .observeOn(scheduler)
                .flatMap(ignored -> likedTracks(INITIAL_TIMESTAMP))
                .subscribeOn(scheduler);
    }

    Observable<List<Urn>> likedTrackUrns() {
        return loadLikedTracksCommand.toObservable(Optional.absent())
                                     .map(likes -> transform(likes, Like::urn)).subscribeOn(scheduler);
    }

}
