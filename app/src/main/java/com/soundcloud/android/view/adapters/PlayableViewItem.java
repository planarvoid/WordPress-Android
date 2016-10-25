package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;

public interface PlayableViewItem {

    Urn getPlayableUrn();

    boolean isPlaying();

    void setIsPlaying(boolean isCurrent);
}
