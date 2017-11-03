package com.soundcloud.android.playback;

import com.soundcloud.android.rx.observers.DefaultSingleObserver;

import javax.inject.Inject;

public class ExpandPlayerSingleObserver extends DefaultSingleObserver<PlaybackResult> {

    private final ExpandPlayerCommand expandPlayerCommand;

    @Inject
    public ExpandPlayerSingleObserver(ExpandPlayerCommand expandPlayerCommand) {
        this.expandPlayerCommand = expandPlayerCommand;
    }

    @Override
    public void onSuccess(PlaybackResult result) {
        expandPlayerCommand.call(result);
        super.onSuccess(result);
    }

}
