package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;

import android.view.View;
import android.view.ViewGroup;

public interface PlayerPagePresenter<T extends PlayerItem> {

    View createItemView(ViewGroup container, SkipListener skipListener);

    View clearItemView(View convertView);

    void bindItemView(View view, T playerItem);

    void setProgress(View trackPage, PlaybackProgress progress);

    void setPlayState(View trackPage,
                      PlayStateEvent playStateEvent,
                      boolean isCurrentTrack,
                      boolean isForeground);

    void onPlayableUpdated(View trackPage, EntityStateChangedEvent trackChangedEvent);

    void onBackground(View trackPage);

    void onForeground(View trackPage);

    void onDestroyView(View trackPage);

    void setCollapsed(View trackPage);

    void setExpanded(View trackPage, PlayQueueItem playQueueItem, boolean isSelected);

    void onPlayerSlide(View trackPage, float position);

    void clearAdOverlay(View trackPage);

    void setCastDeviceName(View trackPage, String deviceName);

    void onViewSelected(View view, PlayQueueItem value, boolean isExpanded);
}
