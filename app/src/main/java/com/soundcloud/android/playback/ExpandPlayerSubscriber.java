package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackOperations.UnSkippablePeriodException;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.ui.view.PlaybackToastViewController;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;

import javax.inject.Inject;
import java.util.List;

public class ExpandPlayerSubscriber extends DefaultSubscriber<List<TrackUrn>> {
    private final EventBus eventBus;
    private final PlaybackToastViewController playbackToastViewController;


    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus, PlaybackToastViewController playbackToastViewController) {
        this.eventBus = eventBus;
        this.playbackToastViewController = playbackToastViewController;
    }

    @Override
    public void onCompleted() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }

    @Override
    public void onError(Throwable e) {
        if (e instanceof UnSkippablePeriodException) {
            playbackToastViewController.showUnkippableAdToast();
        }
    }
}
