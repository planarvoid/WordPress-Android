package com.soundcloud.android.playback.flipper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class FlipperWrapperFactory {

    private final FlipperFactory flipperFactory;

    @Inject
    public FlipperWrapperFactory(FlipperFactory flipperFactory) {
        this.flipperFactory = flipperFactory;
    }

    public FlipperWrapper create(FlipperAdapter flipperAdapter) {
        return new FlipperWrapper(flipperAdapter, flipperFactory);
    }
}
