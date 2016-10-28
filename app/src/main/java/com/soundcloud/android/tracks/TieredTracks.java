package com.soundcloud.android.tracks;

public final class TieredTracks {

    public static boolean isHighTierPreview(TieredTrack track) {
        return track.isSnipped() && track.isSubHighTier();
    }

    public static boolean isFullHighTierTrack(TieredTrack track) {
        return !track.isSnipped() && track.isSubHighTier();
    }

    private TieredTracks() {}

}
