package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Playa;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.view.ViewGroup;

public interface PlayerPagePresenter {

    View createItemView(ViewGroup container, SkipListener skipListener);
    View clearItemView(View convertView);
    void bindItemView(View view, PropertySet propertySet, boolean isCurrentTrack,
                      boolean isForeground, ViewVisibilityProvider viewVisibilityProvider);

    void setProgress(View trackPage, PlaybackProgress progress);
    void setPlayState(View trackPage, Playa.StateTransition stateTransition, boolean isCurrentTrack, boolean isForeground);
    void onPlayableUpdated(View trackPage, EntityStateChangedEvent trackChangedEvent);

    void onBackground(View trackPage);
    void onForeground(View trackPage);

    void setCollapsed(View trackPage);
    void setExpanded(View trackPage);
    void onPlayerSlide(View trackPage, float position);

    void clearAdOverlay(View trackPage);

    void setCastDeviceName(View trackPage, String deviceName);
}
