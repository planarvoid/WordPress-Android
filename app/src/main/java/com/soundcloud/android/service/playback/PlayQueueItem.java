package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

public class PlayQueueItem {
    private Track mTrack;
    private int mPlayQueuePosition;

    public static PlayQueueItem empty(int position){
        return new PlayQueueItem(position);
    }

    PlayQueueItem(int pos) {
        mPlayQueuePosition = pos;
    }

    public PlayQueueItem(Track track, int pos) {
        this(pos);
        mTrack = track;

    }

    public Track getTrack() {
        return mTrack;
    }

    public boolean isEmpty() {
        return mTrack == null;
    }

    public int getPlayQueuePosition() {
        return mPlayQueuePosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayQueueItem that = (PlayQueueItem) o;

        if (mPlayQueuePosition != that.mPlayQueuePosition) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return mPlayQueuePosition;
    }
}
