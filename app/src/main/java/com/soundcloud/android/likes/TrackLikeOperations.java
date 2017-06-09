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
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
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
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final EventBus eventBus;
    private final TrackItemRepository trackRepo;

    @Inject
    public TrackLikeOperations(LoadLikedTracksCommand loadLikedTracksCommand,
                               SyncInitiatorBridge syncInitiatorBridge,
                               EventBus eventBus,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               TrackItemRepository trackItemRepository) {
        this.loadLikedTracksCommand = loadLikedTracksCommand;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
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
        return RxJava.toV1Observable(syncInitiatorBridge.hasSyncedTrackLikesBefore())
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
                                     .flatMap(source -> enrich(source, trackRepo.fromUrns(transform(source, UrnHolder::urn)), COMBINER))
                                     .subscribeOn(scheduler);
    }

    Observable<List<LikeWithTrack>> updatedLikedTracks() {
        return RxJava.toV1Observable(syncInitiatorBridge.syncTrackLikes())
                     .observeOn(scheduler)
                     .flatMap(ignored -> likedTracks(INITIAL_TIMESTAMP))
                     .subscribeOn(scheduler);
    }

    Observable<List<Urn>> likedTrackUrns() {
        return loadLikedTracksCommand.toObservable(Optional.absent())
                                     .map(likes -> transform(likes, Like::urn)).subscribeOn(scheduler);
    }

}
