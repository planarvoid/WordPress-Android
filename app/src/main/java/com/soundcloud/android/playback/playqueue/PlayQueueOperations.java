package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlayQueueOperations {

    private final static Func1<List<PlayQueueItem>, Iterable<? extends Urn>> TO_URN = new Func1<List<PlayQueueItem>, Iterable<? extends Urn>>() {
        @Override
        public Iterable<? extends Urn> call(List<PlayQueueItem> playQueueItems) {
            return Lists.transform(playQueueItems, new Function<PlayQueueItem, Urn>() {
                @Override
                public Urn apply(PlayQueueItem input) {
                    return input.getUrn();
                }
            });
        }
    };

    private static final Predicate<PlayQueueItem> IS_TRACK = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(PlayQueueItem input) {
            return input.isTrack();
        }
    };

    private final Func1<Urn, Observable<PropertySet>> loadTrack = new Func1<Urn, Observable<PropertySet>>() {
        @Override
        public Observable<PropertySet> call(Urn urn) {
            return trackRepository.track(urn);
        }

    };

    private Scheduler scheduler;
    private final PlayQueueManager playQueueManager;
    private final TrackRepository trackRepository;
    private final LoadTrackImageResource loadTrackImageResource;

    @Inject
    public PlayQueueOperations(@Named(HIGH_PRIORITY) Scheduler scheduler,
                               PlayQueueManager playQueueManager,
                               TrackRepository trackRepository,
                               LoadTrackImageResource loadTrackImageResource) {
        this.scheduler = scheduler;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.loadTrackImageResource = loadTrackImageResource;
    }

    public Observable<List<TrackItem>> getTrackItems() {
        return Observable.just(playQueueManager.getPlayQueueItems(IS_TRACK))
                         .flatMapIterable(TO_URN)
                         .concatMapEager(loadTrack)
                         .map(TrackItem.fromPropertySet())
                         .toList()
                         .subscribeOn(scheduler);
    }

    public Observable<ImageResource> getTrackArtworkResource(Urn urn) {
        return loadTrackImageResource.toObservable(urn).subscribeOn(scheduler);
    }

}
