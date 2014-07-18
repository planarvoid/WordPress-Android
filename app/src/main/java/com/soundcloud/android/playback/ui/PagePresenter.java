package com.soundcloud.android.playback.ui;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.view.ViewGroup;

public interface PagePresenter {

    View createItemView(ViewGroup container);
    View clearItemView(View convertView);
    void bindItemView(View view, PropertySet propertySet);
    void setProgress(View trackView, PlaybackProgress progress);
    void setPlayState(View trackPage, Playa.StateTransition stateTransition, boolean viewPresentingCurrentTrack);
    void clearScrubState(View key);

    void setExpanded(View trackPage, boolean playing);
    void setCollapsed(View trackPage);
}
