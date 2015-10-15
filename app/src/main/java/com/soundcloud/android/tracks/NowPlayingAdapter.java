package com.soundcloud.android.tracks;

import com.soundcloud.android.model.Urn;

public interface NowPlayingAdapter {

    void updateNowPlaying(Urn currentlyPlayingUrn);
}
