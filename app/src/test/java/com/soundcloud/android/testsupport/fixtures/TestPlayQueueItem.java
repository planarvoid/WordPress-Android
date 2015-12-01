package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoQueueItem;

public class TestPlayQueueItem {
    public static TrackQueueItem createTrack(Urn itemUrn) {
        return new TrackQueueItem.Builder(itemUrn).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, AdData adData) {
        return new TrackQueueItem.Builder(itemUrn).withAdData(adData).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, AdData adData, Urn relatedEntity) {
        return new TrackQueueItem.Builder(itemUrn).withAdData(adData).relatedEntity(relatedEntity).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, String source, String sourceVersion) {
        return new TrackQueueItem.Builder(itemUrn).fromSource(source, sourceVersion).build();
    }

    public static VideoQueueItem createVideo(VideoAd adData) {
        return new VideoQueueItem(adData);
    }
}
