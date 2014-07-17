package com.soundcloud.android.playback.service.skippy;

import com.soundcloud.android.skippy.Skippy;

import javax.inject.Inject;

public class SkippyFactory {

    @Inject
    SkippyFactory() {
    }

    public Skippy create() {
        return new Skippy();
    }

    public Skippy create(Skippy.PlayListener listener) {
        return new Skippy(listener);
    }
}
