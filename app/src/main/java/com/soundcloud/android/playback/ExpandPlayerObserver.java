package com.soundcloud.android.playback;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DefaultObserver;

import javax.inject.Inject;

public class ExpandPlayerObserver extends DefaultObserver<PlaybackResult> {

    private final ExpandPlayerCommand expandPlayerCommand;

    @Inject
    ExpandPlayerObserver(ExpandPlayerCommand expandPlayerCommand) {
        this.expandPlayerCommand = expandPlayerCommand;
    }

    @Override
    public void onNext(@NonNull PlaybackResult playbackResult) {
        expandPlayerCommand.call(playbackResult);
    }

    @Override
    public void onError(@NonNull Throwable e) {}

    @Override
    public void onComplete() {}

}
