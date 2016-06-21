package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;

public interface PlayingTrackAware {

    void updateNowPlaying(Urn currentlyPlayingUrn);
}
