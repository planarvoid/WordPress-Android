package com.soundcloud.android.playback;

import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;

public final class Durations {

    public static long getTrackPlayDuration(TrackItem track) {
        return track.isSnipped() ?
               track.snippetDuration() :
               track.fullDuration();
    }

    public static long getTrackPlayDuration(Track track) {
        return track.snipped() ?
               track.snippetDuration() :
               track.fullDuration();
    }

}
