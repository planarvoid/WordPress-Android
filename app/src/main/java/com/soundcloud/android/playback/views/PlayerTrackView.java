package com.soundcloud.android.playback.views;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlaybackStateProvider;

public interface PlayerTrackView {

    public void onDataConnected();

    public void onStop();

    public void onDestroy();

    public void setTrackState(Track track, int queuePosition, PlaybackStateProvider stateProvider);

    public long getTrackId();

    void setOnScreen(boolean isOnScreen);
}
