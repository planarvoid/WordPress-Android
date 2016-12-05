package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.collections.ListsFunctions.cast;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueStorage;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.DiffUtils;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Predicate;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayQueueOperations {

    private static final Predicate<PlayQueueItem> IS_TRACK = input -> input != null && input.isTrack() && input.getUrn().isTrack();

    private Scheduler scheduler;
    private final PlayQueueManager playQueueManager;
    private final TrackRepository trackRepository;
    private final PlayQueueStorage playQueueStorage;

    @Inject
    public PlayQueueOperations(@Named(HIGH_PRIORITY) Scheduler scheduler,
                               PlayQueueManager playQueueManager,
                               TrackRepository trackRepository,
                               PlayQueueStorage playQueueStorage) {
        this.scheduler = scheduler;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.playQueueStorage = playQueueStorage;
    }

    public Observable<List<TrackAndPlayQueueItem>> getTracks() {
        return Observable.defer(this::loadTracks);
    }

    public List<PlayQueueItem> getQueueTracks() {
        return playQueueManager.getPlayQueueItems(IS_TRACK);
    }

    Observable<Map<Urn, String>> getContextTitles() {
        return playQueueStorage.contextTitles().subscribeOn(scheduler);
    }

    private Observable<List<TrackAndPlayQueueItem>> loadTracks() {
        final List<TrackQueueItem> playQueueItems = Lists.transform(playQueueManager.getPlayQueueItems(IS_TRACK), cast(TrackQueueItem.class));

        final Func1<Map<Urn, TrackItem>, List<TrackAndPlayQueueItem>> fulfillWithKnownProperties = urnPropertySetMap -> toTrackAndPlayQueueItem(playQueueItems, urnPropertySetMap);

        final List<Urn> uniqueTrackUrns = DiffUtils.deduplicate(transform(playQueueItems, PlayQueueItem.TO_URN));
        return trackRepository
                .tracks(uniqueTrackUrns)
                .map(fulfillWithKnownProperties)
                .subscribeOn(scheduler);
    }

    private ArrayList<TrackAndPlayQueueItem> toTrackAndPlayQueueItem(List<TrackQueueItem> playQueueItems,
                                                                     Map<Urn, TrackItem> knownProperties) {
        final ArrayList<TrackAndPlayQueueItem> trackItems = new ArrayList<>(playQueueItems.size());

        for (TrackQueueItem item : playQueueItems) {
            addTrackAndPlayQueueItemIfPresent(knownProperties, trackItems, item);
        }
        return trackItems;
    }

    private void addTrackAndPlayQueueItemIfPresent(Map<Urn, TrackItem> urnPropertySetMap,
                                                   ArrayList<TrackAndPlayQueueItem> trackItems,
                                                   TrackQueueItem item) {
        final Urn urn = item.getUrn();
        if (urnPropertySetMap.containsKey(urn)) {
            trackItems.add(new TrackAndPlayQueueItem(urnPropertySetMap.get(urn), item));
        }
    }

}
