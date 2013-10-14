package com.soundcloud.android.player;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.DefaultObserver;
import rx.Observable;

class PlayQueueItem {
    private Track mTrack;
    private int mPlayQueuePosition;

    public static PlayQueueItem empty(int position){
        return new PlayQueueItem(position);
    }

    PlayQueueItem(int pos) {
        mPlayQueuePosition = pos;
    }

    public PlayQueueItem(Track track, int pos) {
        mTrack = track;
    }

    public PlayQueueItem(Observable<Track> trackObservable, int pos) {
        this(pos);
        trackObservable.subscribe(new DefaultObserver<Track>() {
            @Override
            public void onNext(Track track) {
                mTrack = track;
            }
            // TODO, error case
        });
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
