package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.playback.PlayQueueManager.RepeatMode.REPEAT_ONE;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlayQueueOperations {

    private final static Func1<List<Urn>, Iterable<? extends Urn>> TO_URN = new Func1<List<Urn>, Iterable<? extends Urn>>() {
        @Override
        public Iterable<? extends Urn> call(List<Urn> urns) {
            return urns;
        }
    };

    private final Func1<Urn, Observable<PropertySet>> toPropertySets = new Func1<Urn, Observable<PropertySet>>() {

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
                               TrackRepository trackRepository, LoadTrackImageResource loadTrackImageResource) {
        this.scheduler = scheduler;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.loadTrackImageResource = loadTrackImageResource;
    }

    public Observable<List<TrackItem>> getTrackItems() {
        return Observable.just(playQueueManager.getCurrentQueueTrackUrns())
                         .flatMapIterable(TO_URN)
                         .concatMapEager(toPropertySets)
                         .map(TrackItem.fromPropertySet())
                         .map(withCurrentRepeatMode())
                         .toList()
                         .subscribeOn(scheduler);
    }

    public Observable<ImageResource> getTrackArtworkResource(Urn urn) {
        return loadTrackImageResource.toObservable(urn).subscribeOn(scheduler);
    }

    private Func1<TrackItem, TrackItem> withCurrentRepeatMode() {
        return new Func1<TrackItem, TrackItem>() {
            @Override
            public TrackItem call(TrackItem trackItem) {
                trackItem.setInRepeatMode(playQueueManager.getRepeatMode() == REPEAT_ONE);
                return trackItem;
            }
        };
    }

}
