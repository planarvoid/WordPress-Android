package com.soundcloud.android.player;

import com.soundcloud.android.model.Track;
import rx.Observable;

class PlayQueueItem {

    private Observable<Track> mTrackObservable;
    private int mPlayQueuePosition;

    // TODO, try to use a static instance since we only have 1 empty item
    public static PlayQueueItem empty(int position){
        return new PlayQueueItem(position);
    }

    PlayQueueItem(int pos) {
        mPlayQueuePosition = pos;
    }

    public PlayQueueItem(Observable<Track> trackObservable, int pos) {
        this(pos);
        mTrackObservable = trackObservable;
    }

    public Observable<Track> getTrack() {
        return mTrackObservable;
    }

    public boolean isEmpty() {
        return mTrackObservable == null;
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
