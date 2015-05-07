package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import javax.inject.Inject;

public class ShowPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {
    private final EventBus eventBus;
    private final PlaybackToastHelper playbackToastHelper;

    @Inject
    public ShowPlayerSubscriber(EventBus eventBus, PlaybackToastHelper playbackToastHelper) {
        this.eventBus = eventBus;
        this.playbackToastHelper = playbackToastHelper;
    }

    @Override
    public void onNext(PlaybackResult result) {
        if (result.isSuccess()) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
        } else {
            playbackToastHelper.showToastOnPlaybackError(result.getErrorReason());
        }
    }

}
