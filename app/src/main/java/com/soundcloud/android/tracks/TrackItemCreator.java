package com.soundcloud.android.tracks;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.StreamEntity;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class TrackItemCreator {

    @Inject
    public TrackItemCreator() {
    }

    public TrackItem trackItem(Track track) {
        return TrackItem.from(track);
    }

    public TrackItem trackItem(Track track, StreamEntity streamEntity) {
        return TrackItem.from(track, streamEntity);
    }

    Map<Urn, TrackItem> convertMap(Map<Urn, Track> map) {
        Map<Urn, TrackItem> trackItemMap = new HashMap<>();
        for(Map.Entry<Urn, Track> entry : map.entrySet()) {
            trackItemMap.put(entry.getKey(), trackItem(entry.getValue()));
        }
        return trackItemMap;
    }
}
