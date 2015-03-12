package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import javax.inject.Inject;
import java.util.List;

public class ShowPlayerSubscriber extends DefaultSubscriber<List<Urn>> {
    private final PlaybackToastHelper playbackToastHelper;
    private final EventBus eventBus;

    @Inject
    public ShowPlayerSubscriber(EventBus eventBus, PlaybackToastHelper playbackToastHelper) {
        this.playbackToastHelper = playbackToastHelper;
        this.eventBus = eventBus;
    }

    @Override
    public void onCompleted() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
    }

    @Override
    public void onError(Throwable e) {
       playbackToastHelper.showToastOnPlaybackError(e);
    }
}
