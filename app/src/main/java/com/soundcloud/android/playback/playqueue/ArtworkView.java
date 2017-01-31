package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;

import android.support.v4.app.Fragment;

public abstract class ArtworkView extends SupportFragmentLightCycleDispatcher<Fragment> {

    abstract void setImage(ImageResource imageResource);

    abstract void cancelProgressAnimation();

    abstract void startProgressAnimation(PlaybackProgress progress, long duration);

    abstract void setPlaybackProgress(PlaybackProgress progress, long duration);

    abstract void setProgressControllerValues(int startX, int endX);
}
