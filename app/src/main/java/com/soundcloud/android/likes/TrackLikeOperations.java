package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.utils.RepoUtils.enrichV2;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class TrackLikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;
    static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;
    private static final BiFunction<TrackItem, Like, LikeWithTrack> COMBINER = (trackItem, like) -> LikeWithTrack
            .create(like, trackItem);

    private final LikesStorage likesStorage;
    private final Scheduler scheduler;
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final EventBusV2 eventBus;
    private final TrackItemRepository trackRepo;

    @Inject
    public TrackLikeOperations(LikesStorage likesStorage,
                               SyncInitiatorBridge syncInitiatorBridge,
                               EventBusV2 eventBus,
                               @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                               TrackItemRepository trackItemRepository) {
        this.likesStorage = likesStorage;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.trackRepo = trackItemRepository;
    }

    Observable<TrackItem> onTrackLiked() {
        return singleTrackLikeStatusChange(true).flatMapMaybe(trackRepo::track);
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

    Single<List<LikeWithTrack>> likedTracks() {
        return syncInitiatorBridge.hasSyncedTrackLikesBefore()
                                  .flatMap(hasSynced -> {
                                      if (hasSynced) {
                                          return likedTracks(INITIAL_TIMESTAMP);
                                      } else {
                                          return updatedLikedTracks();
                                      }
                                  }).subscribeOn(scheduler);
    }

    Single<List<LikeWithTrack>> likedTracks(long beforeTime) {
        return likesStorage.loadTrackLikes(beforeTime, PAGE_SIZE)
                           .flatMap(source -> enrichV2(source, trackRepo.fromUrns(transform(source, UrnHolder::urn)), COMBINER))
                           .subscribeOn(scheduler);
    }

    Single<List<LikeWithTrack>> updatedLikedTracks() {
        return syncInitiatorBridge.syncTrackLikes()
                                  .flatMap(ignored -> likedTracks(INITIAL_TIMESTAMP));
    }

    Single<List<Urn>> likedTrackUrns() {
        return likesStorage.loadTrackLikes()
                           .map(likes -> transform(likes, Like::urn))
                           .subscribeOn(scheduler);
    }

}
