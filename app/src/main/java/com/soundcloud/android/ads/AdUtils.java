package com.soundcloud.android.ads;

import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.java.functions.Predicate;

public final class AdUtils {

    private AdUtils() {}

    public static final Predicate<PlayQueueItem> IS_PLAYER_AD_ITEM = input -> input.isAd();

    public static final Predicate<PlayQueueItem> IS_NOT_AD = input -> !input.isAd();

    public static final Predicate<PlayQueueItem> IS_AUDIO_AD_ITEM = input -> input.isAudioAd();

    public static boolean isAd(PlaybackItem playbackItem) {
        return playbackItem.getPlaybackType() == PlaybackType.AUDIO_AD
                || playbackItem.getPlaybackType() == PlaybackType.VIDEO_AD
                || playbackItem.getUrn().isAd();
    }

    public static boolean hasAdOverlay(PlayQueueItem playQueueItem) {
        return playQueueItem.getAdData().isPresent()
                && playQueueItem.getAdData().get() instanceof OverlayAdData;
    }

}
