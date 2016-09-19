package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.TestDateProvider;

public class TestPlaybackProgress {

    public static PlaybackProgress empty(CurrentDateProvider dateProvider) {
        return new PlaybackProgress(0, 0, dateProvider, TestPlayStates.URN);
    }

    public static PlaybackProgress getPlaybackProgress(long position, long duration) {
        return new PlaybackProgress(position, duration, TestPlayStates.URN);
    }

    public static PlaybackProgress getPlaybackProgress(int position, int duration) {
        return getPlaybackProgress(position, duration, new TestDateProvider());
    }

    public static PlaybackProgress getPlaybackProgress(int position, int duration, DateProvider dateProvider) {
        return new PlaybackProgress(position, duration, dateProvider, TestPlayStates.URN);
    }
}
