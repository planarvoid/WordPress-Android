package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.view.ViewGroup;

public interface PagePresenter {

    View createItemView(ViewGroup container, SkipListener skipListener);
    View clearItemView(View convertView);
    void bindItemView(View view, PropertySet propertySet);

    void setProgress(View trackPage, PlaybackProgress progress);
    void setPlayState(View trackPage, Playa.StateTransition stateTransition, boolean viewPresentingCurrentTrack);
    void onPlayableUpdated(View trackPage, PlayableUpdatedEvent playableUpdatedEvent);

    void setCollapsed(View trackPage);
    void setExpanded(View trackPage);
    void onPlayerSlide(View trackPage, float position);
}
