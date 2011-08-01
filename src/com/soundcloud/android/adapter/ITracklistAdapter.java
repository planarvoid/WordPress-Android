package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Track;

/**
 * Created by IntelliJ IDEA. User: jschmidt Date: 6/9/11 Time: 2:41 PM To change this template use File | Settings |
 * File Templates.
 */
public interface ITracklistAdapter {
    public Track getTrackAt(int index);
    boolean isPlaying();
    public void setPlayingId(long currentTrackId, boolean isPlaying);
    long getPlayingId();
    void removeFavorite(Track track);
    void addFavorite(Track track);
}
