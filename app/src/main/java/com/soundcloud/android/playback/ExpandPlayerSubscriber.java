package com.soundcloud.android.playback;

import com.soundcloud.android.rx.observers.DefaultSubscriber;

import javax.inject.Inject;

/**
 * Should be deleted after RxJava2 migration.
 *
 * @deprecated Use {@link ExpandPlayerSingleObserver} or {@link ExpandPlayerObserver} instead.
 */
@Deprecated
public class ExpandPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {

    private final ExpandPlayerCommand expandPlayerCommand;

    @Inject
    public ExpandPlayerSubscriber(ExpandPlayerCommand expandPlayerCommand) {
        this.expandPlayerCommand = expandPlayerCommand;
    }

    @Override
    public void onNext(PlaybackResult result) {
        expandPlayerCommand.call(result);
    }

}
