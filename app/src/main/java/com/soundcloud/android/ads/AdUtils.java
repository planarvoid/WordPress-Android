package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.java.functions.Predicate;

public final class AdUtils {

    private AdUtils() {}

    public static final Predicate<PlayQueueItem> IS_PLAYER_AD_ITEM = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(PlayQueueItem input) {
            return isAd(input);
        }
    };

    public static final Predicate<PlayQueueItem> IS_AUDIO_AD_ITEM = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(PlayQueueItem input) {
            return isAudioAd(input);
        }
    };

    public static boolean isAd(PlayQueueItem playQueueItem) {
        return isAudioAd(playQueueItem) || isVideoAd(playQueueItem);
    }

    public static boolean isAd(PlaybackItem playbackItem) {
        return playbackItem.getPlaybackType() == PlaybackType.AUDIO_AD
                || playbackItem.getPlaybackType() == PlaybackType.VIDEO_AD
                || playbackItem.getUrn().isAd();
    }

    public static boolean isAudioAd(PlayQueueItem playQueueItem) {
        return playQueueItem.getAdData().isPresent() && playQueueItem.getAdData().get() instanceof AudioAd;
    }

    public static boolean isVideoAd(PlayQueueItem playQueueItem) {
        return playQueueItem.isVideo();
    }

    public static boolean hasAdOverlay(PlayQueueItem playQueueItem) {
        return playQueueItem.getAdData().isPresent() && playQueueItem.getAdData().get() instanceof OverlayAdData;
    }

    public static boolean isThirdPartyAudioAd(Urn trackUrn) {
        return trackUrn.equals(AdConstants.THIRD_PARTY_AD_MAGIC_TRACK_URN);
    }
}
