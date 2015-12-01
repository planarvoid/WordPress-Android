package com.soundcloud.android.ads;

import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.java.functions.Predicate;

public final class AdFunctions {

    private AdFunctions() {}

    public static final Predicate<PlayQueueItem> IS_PLAYER_AD_ITEM = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(PlayQueueItem input) {
            return input.getAdData().isPresent() && input.getAdData().get() instanceof PlayerAdData;
        }
    };

    public static final Predicate<PlayQueueItem> IS_AUDIO_AD_ITEM = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(PlayQueueItem input) {
            return input.getAdData().isPresent() && input.getAdData().get() instanceof AudioAd;
        }
    };
}
