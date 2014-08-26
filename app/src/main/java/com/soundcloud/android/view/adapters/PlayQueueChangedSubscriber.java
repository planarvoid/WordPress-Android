package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlayQueueChangedSubscriber extends DefaultSubscriber<List<TrackUrn>> {
    private final EventBus eventBus;
    private final Func1<Long, PlayerUIEvent> toPlayerExpandEvent = new Func1<Long, PlayerUIEvent>() {
        @Override
        public PlayerUIEvent call(Long aLong) {
            return PlayerUIEvent.forExpandPlayer();
        }
    };

    public PlayQueueChangedSubscriber(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onCompleted() {
        Observable.timer(100, TimeUnit.MILLISECONDS)
                .map(toPlayerExpandEvent)
                .subscribe(eventBus.queue(EventQueue.PLAYER_UI));
    }
}
