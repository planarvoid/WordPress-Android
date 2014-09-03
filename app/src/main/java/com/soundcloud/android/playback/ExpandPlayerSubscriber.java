package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackOperations.UnSkippablePeriodException;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.ui.view.PlaybackToastViewController;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;

import android.os.Handler;
import android.os.Message;

import javax.inject.Inject;
import java.util.List;

public class ExpandPlayerSubscriber extends DefaultSubscriber<List<TrackUrn>> {
    public static final int EXPAND_DELAY_MILLIS = 100;
    private final EventBus eventBus;
    private final PlaybackToastViewController playbackToastViewController;

    private final Handler expandDelayHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
            eventBus.publish(EventQueue.UI, UIEvent.fromPlayerOpen(UIEvent.METHOD_TRACK_PLAY));
        }
    };

    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus, PlaybackToastViewController playbackToastViewController) {
        this.eventBus = eventBus;
        this.playbackToastViewController = playbackToastViewController;
    }

    @Override
    public void onCompleted() {
        expandDelayHandler.sendEmptyMessageDelayed(0, EXPAND_DELAY_MILLIS);
    }

    @Override
    public void onError(Throwable e) {
        if (e instanceof UnSkippablePeriodException) {
            playbackToastViewController.showUnkippableAdToast();
        }
    }
}
