package com.soundcloud.android.playback.ui.progress;

import com.soundcloud.android.events.PlaybackProgress;

public interface ProgressAware {
    void setProgress(PlaybackProgress progress);
}
