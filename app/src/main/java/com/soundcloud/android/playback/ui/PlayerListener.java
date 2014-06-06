package com.soundcloud.android.playback.ui;

import com.soundcloud.android.playback.PlaybackOperations;

import javax.inject.Inject;

class PlayerListener implements PlayerPresenter.Listener {

    private final PlaybackOperations playbackOperations;

    @Inject
    public PlayerListener(PlaybackOperations playbackOperations) {
        this.playbackOperations = playbackOperations;
    }

    @Override
    public void onTrackChanged(int position) {
        playbackOperations.setPlayQueuePosition(position);
    }

}
