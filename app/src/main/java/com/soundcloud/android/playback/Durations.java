package com.soundcloud.android.playback;

import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

public final class Durations {

    public static long getTrackPlayDuration(PropertySet track){
        return track.get(TrackProperty.SNIPPED) ? track.get(TrackProperty.SNIPPET_DURATION) : track.get(TrackProperty.FULL_DURATION);
    }

}
