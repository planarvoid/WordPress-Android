package com.soundcloud.android.playback.service;

public class PlaybackProgressInfo {
    private long mTrackId;
    private long mTime;

    public PlaybackProgressInfo(long trackId, long time) {
        this.mTrackId = trackId;
        this.mTime = time;
    }

    public long getTrackId() {
        return mTrackId;
    }

    public long getTime() {
        return mTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlaybackProgressInfo that = (PlaybackProgressInfo) o;

        if (mTime != that.mTime) return false;
        if (mTrackId != that.mTrackId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (mTrackId ^ (mTrackId >>> 32));
        result = 31 * result + (int) (mTime ^ (mTime >>> 32));
        return result;
    }
}
