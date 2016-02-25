package com.soundcloud.android.tracks;

public final class TieredTracks {

    public static boolean isTrackPreview(TieredTrack track) {
        return track.isSnipped() && (track.isSubHighTier() || track.isSubMidTier());
    }

    public static boolean isHighTierPreview(TieredTrack track) {
        return track.isSnipped() && track.isSubHighTier();
    }

    private TieredTracks() {
    }
}
