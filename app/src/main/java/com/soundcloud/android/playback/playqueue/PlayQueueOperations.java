package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.collections.ListsFunctions.cast;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueStorage;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.utils.DiffUtils;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayQueueOperations {

    private final Scheduler scheduler;
    private final PlayQueueManager playQueueManager;
    private final TrackItemRepository trackItemRepository;
    private final PlayQueueStorage playQueueStorage;

    @Inject
    public PlayQueueOperations(@Named(RX_HIGH_PRIORITY) Scheduler scheduler,
                               PlayQueueManager playQueueManager,
                               TrackItemRepository trackItemRepository,
                               PlayQueueStorage playQueueStorage) {
        this.scheduler = scheduler;
        this.playQueueManager = playQueueManager;
        this.trackItemRepository = trackItemRepository;
        this.playQueueStorage = playQueueStorage;
    }

    public Single<List<TrackAndPlayQueueItem>> getTracks() {
        return Single.defer(this::loadTracks);
    }

    Single<Map<Urn, String>> getContextTitles() {
        return playQueueStorage.contextTitles().subscribeOn(scheduler);
    }

    private Single<List<TrackAndPlayQueueItem>> loadTracks() {
        final List<TrackQueueItem> playQueueItems = Lists.transform(playQueueManager.getPlayQueueItems(input -> input != null && input.isTrack() && input.getUrn().isTrack()),
                                                                    cast(TrackQueueItem.class));

        final Function<Map<Urn, TrackItem>, List<TrackAndPlayQueueItem>> fulfillWithKnownProperties = urnTrackMap -> toTrackAndPlayQueueItem(playQueueItems, urnTrackMap);
        final List<Urn> uniqueTrackUrns = DiffUtils.deduplicate(transform(playQueueItems, PlayQueueItem.TO_URN));

        return trackItemRepository
                .fromUrns(uniqueTrackUrns)
                .map(fulfillWithKnownProperties)
                .subscribeOn(scheduler);
    }

    private List<TrackAndPlayQueueItem> toTrackAndPlayQueueItem(List<TrackQueueItem> playQueueItems,
                                                                Map<Urn, TrackItem> knownProperties) {
        final ArrayList<TrackAndPlayQueueItem> trackItems = new ArrayList<>(playQueueItems.size());

        for (TrackQueueItem item : playQueueItems) {
            addTrackAndPlayQueueItemIfPresent(knownProperties, trackItems, item);
        }
        return trackItems;
    }

    private void addTrackAndPlayQueueItemIfPresent(Map<Urn, TrackItem> urnTrackMap,
                                                   ArrayList<TrackAndPlayQueueItem> trackItems,
                                                   TrackQueueItem item) {
        final Urn urn = item.getUrn();
        if (urnTrackMap.containsKey(urn)) {
            trackItems.add(new TrackAndPlayQueueItem(urnTrackMap.get(urn), item));
        }
    }
}
