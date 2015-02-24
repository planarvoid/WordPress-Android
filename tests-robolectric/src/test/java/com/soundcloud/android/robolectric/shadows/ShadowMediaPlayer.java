package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implements;

import android.media.MediaPlayer;

@Implements(MediaPlayer.class)
public class ShadowMediaPlayer extends com.xtremelabs.robolectric.shadows.ShadowMediaPlayer {
    @Override
    public void prepareAsync() {
        super.prepareAsync();
        invokePreparedListener();
    }
}
