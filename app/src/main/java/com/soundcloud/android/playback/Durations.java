package com.soundcloud.android.playback;

import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

public final class Durations {

    public static long getTrackPlayDuration(PropertySet track) {
        return track.get(TrackProperty.SNIPPED) ?
               track.get(TrackProperty.SNIPPET_DURATION) :
               track.get(TrackProperty.FULL_DURATION);
    }

    public static long getTrackPlayDuration(TrackItem track) {
        return track.isSnipped() ?
               track.getSnippetDuration() :
               track.getFullDuration();
    }

    public static long getTrackPlayDuration(Track track) {
        return track.snipped() ?
               track.snippetDuration() :
               track.fullDuration();
    }

}
