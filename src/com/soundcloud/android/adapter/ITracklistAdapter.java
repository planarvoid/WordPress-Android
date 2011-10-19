package com.soundcloud.android.adapter;

import android.os.Parcelable;
import android.view.View;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import java.util.List;

public interface ITracklistAdapter {
    public List<Parcelable> getData();
    public Track getTrackAt(int index);
    boolean isPlaying();
    public void setPlayingId(long currentTrackId, boolean isPlaying);
    long getPlayingId();
    void removeFavorite(Track track);
    void addFavorite(Track track);
    public QuickTrackMenu getQuickTrackMenu();

}
