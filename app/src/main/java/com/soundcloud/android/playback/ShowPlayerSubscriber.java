package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackOperations.UnskippablePeriodException;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.AdToastViewController;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import java.util.List;

public class ShowPlayerSubscriber extends DefaultSubscriber<List<Urn>> {
    private final AdToastViewController adToastViewController;
    private final EventBus eventBus;

    @Inject
    public ShowPlayerSubscriber(EventBus eventBus, AdToastViewController adToastViewController) {
        this.adToastViewController = adToastViewController;
        this.eventBus = eventBus;
    }

    @Override
    public void onCompleted() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
    }

    @Override
    public void onError(Throwable e) {
        if (e instanceof UnskippablePeriodException) {
            adToastViewController.showUnskippableAdToast();
        }
    }
}
