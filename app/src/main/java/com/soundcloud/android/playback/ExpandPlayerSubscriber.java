package com.soundcloud.android.playback;

import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class ExpandPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {

    private final EventBus eventBus;
    private final PlaybackToastHelper playbackToastHelper;
    private final MiniplayerExperiment miniplayerExperiment;

    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus, PlaybackToastHelper playbackToastHelper, MiniplayerExperiment miniplayerExperiment) {
        this.eventBus = eventBus;
        this.playbackToastHelper = playbackToastHelper;
        this.miniplayerExperiment = miniplayerExperiment;
    }

    @Override
    public void onNext(PlaybackResult result) {
        if (result.isSuccess()) {
            expandPlayer();
        } else {
            playbackToastHelper.showToastOnPlaybackError(result.getErrorReason());
        }
    }

    protected void expandPlayer() {
        if (miniplayerExperiment.canExpandPlayer()) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        } else {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
        }
    }

}
