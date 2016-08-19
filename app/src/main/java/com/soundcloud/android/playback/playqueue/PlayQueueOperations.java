package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.DiffUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayQueueOperations {

    private static final Predicate<PlayQueueItem> IS_TRACK = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(PlayQueueItem input) {
            return input.isTrack();
        }
    };

    private final Func0<Observable<List<TrackAndPlayQueueItem>>> loadTracks = new Func0<Observable<List<TrackAndPlayQueueItem>>>() {
        @Override
        public Observable<List<TrackAndPlayQueueItem>> call() {
            return loadTracks();
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

    public Observable<List<TrackAndPlayQueueItem>> getTracks() {
        return Observable.defer(loadTracks);
    }

    private Observable<List<TrackAndPlayQueueItem>> loadTracks() {
        final List<PlayQueueItem> playQueueItems = playQueueManager.getPlayQueueItems(IS_TRACK);

        final Func1<Map<Urn, PropertySet>, List<TrackAndPlayQueueItem>> fulfillWithKnownProperties = new Func1<Map<Urn, PropertySet>, List<TrackAndPlayQueueItem>>() {
            @Override
            public List<TrackAndPlayQueueItem> call(Map<Urn, PropertySet> urnPropertySetMap) {
                return toTrackAndPlayQueueItem(playQueueItems, urnPropertySetMap);
            }
        };

        final List<Urn> uniqueTrackUrns = DiffUtils.deduplicate(transform(playQueueItems, PlayQueueItem.TO_URN));
        return trackRepository
                .tracks(uniqueTrackUrns)
                .map(fulfillWithKnownProperties)
                .subscribeOn(scheduler);
    }

    private ArrayList<TrackAndPlayQueueItem> toTrackAndPlayQueueItem(List<PlayQueueItem> playQueueItems,
                                                                     Map<Urn, PropertySet> knownProperties) {
        final ArrayList<TrackAndPlayQueueItem> trackItems = new ArrayList<>(playQueueItems.size());

        for (PlayQueueItem item : playQueueItems) {
            addTrackAndPlayQueueItemIfPresent(knownProperties, trackItems, item);
        }
        return trackItems;
    }

    private void addTrackAndPlayQueueItemIfPresent(Map<Urn, PropertySet> urnPropertySetMap,
                                                   ArrayList<TrackAndPlayQueueItem> trackItems,
                                                   PlayQueueItem item) {
        final Urn urn = item.getUrn();
        if (urnPropertySetMap.containsKey(urn)) {
            final PropertySet propertyBindings = urnPropertySetMap.get(urn);
            trackItems.add(new TrackAndPlayQueueItem(TrackItem.from(propertyBindings), item));
        }
    }

    public Observable<ImageResource> getTrackArtworkResource(Urn urn) {
        return loadTrackImageResource.toObservable(urn).subscribeOn(scheduler);
    }

}
