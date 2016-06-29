package com.soundcloud.android.playback.playqueue;

import javax.inject.Inject;

public class PlayQueueFragmentFactory {

    @Inject
    public PlayQueueFragmentFactory() {
    }

    public PlayQueueFragment create() {
        return new PlayQueueFragment();
    }

}
