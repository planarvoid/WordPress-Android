package com.soundcloud.android.playback.ui;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.view.ViewGroup;

public interface PagePresenter {

    public View createItemView(ViewGroup container);
    public View clearItemView(View convertView);
    public void bindItemView(View view, PropertySet propertySet);
    public void setProgress(View trackView, PlaybackProgress progress);
    public void setPlayState(View trackPage, Playa.StateTransition stateTransition, boolean viewPresentingCurrentTrack);
    public void clearScrubState(View key);

    public void setExpanded(View trackPage, boolean playing);
    public void setCollapsed(View trackPage);
}
