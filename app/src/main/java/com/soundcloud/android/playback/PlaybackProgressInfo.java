package com.soundcloud.android.playback;


import com.soundcloud.android.model.Urn;

public class PlaybackProgressInfo {
    private final long trackId;
    private final long time;

    public PlaybackProgressInfo(long trackId, long time) {
        this.trackId = trackId;
        this.time = time;
    }

    @Deprecated // use URNs
    public long getTrackId() {
        return trackId;
    }

    public long getTime() {
        return time;
    }

    public boolean shouldResumeTrack(Urn trackUrn) {
        return getTrackId() == trackUrn.getNumericId() && time > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlaybackProgressInfo that = (PlaybackProgressInfo) o;

        if (time != that.time) {
            return false;
        }
        if (trackId != that.trackId) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (trackId ^ (trackId >>> 32));
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }
}
