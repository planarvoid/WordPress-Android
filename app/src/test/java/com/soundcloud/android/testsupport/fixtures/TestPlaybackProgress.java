package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.utils.CurrentDateProvider;

public class TestPlaybackProgress {
    public static PlaybackProgress empty(CurrentDateProvider dateProvider) {
        return new PlaybackProgress(0, 0, dateProvider);
    }
}
