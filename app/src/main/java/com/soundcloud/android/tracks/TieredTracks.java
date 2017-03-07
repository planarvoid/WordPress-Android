package com.soundcloud.android.tracks;

public final class TieredTracks {

    public static boolean isHighTierPreview(TrackItem trackItem) {
        return trackItem.isSnipped() && trackItem.isSubHighTier();
    }

    public static boolean isFullHighTierTrack(TrackItem trackItem) {
        return !trackItem.isSnipped() && trackItem.isSubHighTier();
    }

    private TieredTracks() {}

}
