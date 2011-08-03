package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Track;

public interface ITracklistAdapter {
    public Track getTrackAt(int index);
    boolean isPlaying();
    public void setPlayingId(long currentTrackId, boolean isPlaying);
    long getPlayingId();
    void removeFavorite(Track track);
    void addFavorite(Track track);
}
