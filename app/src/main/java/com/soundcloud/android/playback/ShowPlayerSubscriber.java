package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class ShowPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {
    private final EventBus eventBus;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;

    @Inject
    public ShowPlayerSubscriber(EventBus eventBus, PlaybackFeedbackHelper playbackFeedbackHelper) {
        this.eventBus = eventBus;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
    }

    @Override
    public void onNext(PlaybackResult result) {
        if (result.isSuccess()) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        } else {
            playbackFeedbackHelper.showFeedbackOnPlaybackError(result.getErrorReason());
        }
    }

}
