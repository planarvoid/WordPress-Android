package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioAdQueueItem;
import com.soundcloud.android.playback.PlaylistQueueItem;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoAdQueueItem;

public class TestPlayQueueItem {

    public static PlaylistQueueItem createPlaylist(Urn itemUrn) {
        return new PlaylistQueueItem.Builder(itemUrn).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn) {
        return new TrackQueueItem.Builder(itemUrn).build();
    }

    public static TrackQueueItem createBlockedTrack(Urn itemUrn) {
        return new TrackQueueItem.Builder(itemUrn).blocked(true).build();
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

    public static VideoAdQueueItem createVideo(VideoAd adData) {
        return new VideoAdQueueItem(adData);
    }

    public static AudioAdQueueItem createAudioAd(AudioAd adData) {
        return new AudioAdQueueItem(adData);
    }
}
