package com.soundcloud.android.playback.ui.progress;

import com.soundcloud.android.playback.PlaybackProgress;

public interface ProgressAware {
    void setProgress(PlaybackProgress progress);
    void clearProgress();
}
