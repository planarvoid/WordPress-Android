package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

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
            // Show the player in it's collapsed state
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
        } else {
            playbackToastHelper.showToastOnPlaybackError(result.getErrorReason());
        }
    }

}
