package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioAdQueueItem;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.PlaylistQueueItem;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoAdQueueItem;

import android.support.annotation.NonNull;

public class TestPlayQueueItem {

    public static PlaylistQueueItem createPlaylist(Urn itemUrn) {
        return new PlaylistQueueItem.Builder(itemUrn).withPlaybackContext(PlaybackContext.create(PlaySessionSource.EMPTY)).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn) {
        return builder(itemUrn).build();
    }

    @NonNull
    private static TrackQueueItem.Builder builder(Urn itemUrn) {
        return new TrackQueueItem.Builder(itemUrn).withPlaybackContext(PlaybackContext.create(PlaySessionSource.EMPTY));
    }

    public static TrackQueueItem createBlockedTrack(Urn itemUrn) {
        return builder(itemUrn).blocked(true).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, OverlayAdData adData) {
        return builder(itemUrn).withAdData(adData).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, AdData adData, Urn relatedEntity) {
        return builder(itemUrn).withAdData(adData).relatedEntity(relatedEntity).build();
    }

    public static TrackQueueItem createTrack(Urn itemUrn, String source, String sourceVersion) {
        return builder(itemUrn).fromSource(source, sourceVersion).build();
    }

   public static TrackQueueItem createTrackWithContext(Urn track, PlaybackContext playbackContext) {
        return new TrackQueueItem.Builder(track)
                .withPlaybackContext(playbackContext)
                .build();
    }

    public static VideoAdQueueItem createVideo(VideoAd adData) {
        return new VideoAdQueueItem(adData);
    }

    public static AudioAdQueueItem createAudioAd(AudioAd adData) {
        return new AudioAdQueueItem(adData);
    }
}
