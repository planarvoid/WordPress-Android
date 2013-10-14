package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;

class PlayQueueItem {
    private Track mTrack;
    private TrackSourceInfo mTrackSourceInfo;

    public PlayQueueItem(Track track) {
        this(track, TrackSourceInfo.EMPTY);
    }

    public PlayQueueItem(Track track, TrackSourceInfo trackSourceInfo) {
        mTrack = track;
        mTrackSourceInfo = trackSourceInfo;
    }

    public Track getTrack() {
        return mTrack;
    }

    public boolean isEmpty() {
        return mTrack == null;
    }

    public TrackSourceInfo getTrackSourceInfo() {
        return mTrackSourceInfo;
    }
}
