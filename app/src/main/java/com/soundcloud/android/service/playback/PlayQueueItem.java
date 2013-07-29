package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

public class PlayQueueItem {
    private Track mTrack;
    private int mPlayQueuePosition;
    private boolean mCommentingMode;

    public PlayQueueItem(Track track, int pos) {
        mTrack = track;
        mPlayQueuePosition = pos;
    }

    public Track getTrack() {
        return mTrack;
    }

    public void setTrack(Track track) {
        mTrack = track;
    }

    public int getPlayQueuePosition() {
        return mPlayQueuePosition;
    }

    public void setPlayQueuePosition(int playQueuePosition) {
        mPlayQueuePosition = playQueuePosition;
    }

    public void setIsInCommentingMode(boolean commentingMode) {
        mCommentingMode = commentingMode;
    }
}
