package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;

import java.util.List;

public class PlayQueueChangedSubscriber extends DefaultSubscriber<List<TrackUrn>> {
    private final EventBus eventBus;

    public PlayQueueChangedSubscriber(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onCompleted() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }
}
