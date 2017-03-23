package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.playback.PlaybackProgress;

public class NoOpArtworkView extends ArtworkView {

    @Override
    void setImage(ImageResource imageResource) {}

    @Override
    void cancelProgressAnimation() {}

    @Override
    void startProgressAnimation(PlaybackProgress progress, long duration) {}

    @Override
    void setPlaybackProgress(PlaybackProgress progress, long duration) {}

    @Override
    void resetProgress() {}

    @Override
    void setProgressControllerValues(int startX, int endX) {}

}
