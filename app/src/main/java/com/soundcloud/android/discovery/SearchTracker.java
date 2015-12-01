package com.soundcloud.android.discovery;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

class SearchTracker {

    private final EventBus eventBus;

    @Inject
    SearchTracker(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    void trackScreenEvent() {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_MAIN));
    }
}
