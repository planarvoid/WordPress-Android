package com.soundcloud.android.playback;

import com.soundcloud.android.ads.PlayerAdData;

public interface AdPlaybackItem extends PlaybackItem {

    PlayerAdData getAdData();

}
