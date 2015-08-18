package com.soundcloud.android.analytics;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ScreenProvider {

    private final EventBus eventBus;
    private final Func1<TrackingEvent, Boolean> IS_SCREEN_TRACKING = new Func1<TrackingEvent, Boolean>() {
        @Override
        public Boolean call(TrackingEvent trackingEvent) {
            return trackingEvent instanceof ScreenEvent;
        }
    };

    private String lastScreenTag;

    @Inject
    public ScreenProvider(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.TRACKING)
                .filter(IS_SCREEN_TRACKING)
                .cast(ScreenEvent.class)
                .subscribe(new ScreenTrackingEventSubscriber());
    }

    public String getLastScreenTag() {
        return lastScreenTag;
    }

    private class ScreenTrackingEventSubscriber extends DefaultSubscriber<ScreenEvent> {
        @Override
        public void onNext(ScreenEvent event) {
            lastScreenTag = event.getScreenTag();
        }
    }
}
