package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoQueueItem;
import com.soundcloud.java.collections.PropertySet;

public class TestPlayQueueItem {
    public static TrackQueueItem createTrack(Urn itemUrn) {
        return new TrackQueueItem.Builder(itemUrn).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, PropertySet metaData) {
        return new TrackQueueItem.Builder(itemUrn).withAdData(metaData).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, PropertySet metaData, Urn relatedEntity) {
        return new TrackQueueItem.Builder(itemUrn).withAdData(metaData).relatedEntity(relatedEntity).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, String source, String sourceVersion) {
        return new TrackQueueItem.Builder(itemUrn).fromSource(source, sourceVersion).build();
    }

    public static VideoQueueItem createVideo(PropertySet metaData) {
        return new VideoQueueItem(metaData);
    }
}
