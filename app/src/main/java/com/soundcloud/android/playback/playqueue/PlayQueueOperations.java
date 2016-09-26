package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueStorage;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.DiffUtils;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;
import rx.functions.Func1;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayQueueOperations {

    private static final Predicate<PlayQueueItem> IS_TRACK = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(@Nullable PlayQueueItem input) {
            return input != null && input.isTrack() && input.getUrn().isTrack();
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
    private final PlayQueueStorage playQueueStorage;

    @Inject
    public PlayQueueOperations(@Named(HIGH_PRIORITY) Scheduler scheduler,
                               PlayQueueManager playQueueManager,
                               TrackRepository trackRepository,
                               LoadTrackImageResource loadTrackImageResource,
                               PlayQueueStorage playQueueStorage) {
        this.scheduler = scheduler;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.loadTrackImageResource = loadTrackImageResource;
        this.playQueueStorage = playQueueStorage;
    }

    public Observable<List<TrackAndPlayQueueItem>> getTracks() {
        return Observable.defer(loadTracks);
    }

    Observable<Map<Urn, String>> getContextTitles() {
        return playQueueStorage.contextTitles().subscribeOn(scheduler);
    }

    private Observable<List<TrackAndPlayQueueItem>> loadTracks() {
        final List<TrackQueueItem> playQueueItems = Lists.transform(playQueueManager.getPlayQueueItems(IS_TRACK), cast(TrackQueueItem.class));

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

    private static <T extends PlayQueueItem> Function<PlayQueueItem, T> cast(final Class<T> clazz) {
        return new Function<PlayQueueItem, T>() {
            @Override
            public T apply(PlayQueueItem input) {
                return clazz.cast(input);
            }
        };
    }

    private ArrayList<TrackAndPlayQueueItem> toTrackAndPlayQueueItem(List<TrackQueueItem> playQueueItems,
                                                                     Map<Urn, PropertySet> knownProperties) {
        final ArrayList<TrackAndPlayQueueItem> trackItems = new ArrayList<>(playQueueItems.size());

        for (TrackQueueItem item : playQueueItems) {
            addTrackAndPlayQueueItemIfPresent(knownProperties, trackItems, item);
        }
        return trackItems;
    }

    private void addTrackAndPlayQueueItemIfPresent(Map<Urn, PropertySet> urnPropertySetMap,
                                                   ArrayList<TrackAndPlayQueueItem> trackItems,
                                                   TrackQueueItem item) {
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
