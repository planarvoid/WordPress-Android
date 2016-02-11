package com.soundcloud.android.discovery;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;


/**
 * Design discussion: https://github.com/soundcloud/SoundCloud-Android/pull/4845
 */
@Singleton
public class SearchTracker {

    private final EventBus eventBus;

    private final Map<Screen, Urn> screenQueryUrnMap;

    @Inject
    public SearchTracker(EventBus eventBus) {
        this.eventBus = eventBus;
        this.screenQueryUrnMap = new EnumMap<>(Screen.class);
        initializeScreenQueryUrnMap();
    }

    private void initializeScreenQueryUrnMap() {
        this.screenQueryUrnMap.put(Screen.SEARCH_EVERYTHING, Urn.NOT_SET);
        this.screenQueryUrnMap.put(Screen.SEARCH_TRACKS, Urn.NOT_SET);
        this.screenQueryUrnMap.put(Screen.SEARCH_PLAYLISTS, Urn.NOT_SET);
        this.screenQueryUrnMap.put(Screen.SEARCH_USERS, Urn.NOT_SET);
    }

    public void trackMainScreenEvent() {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_MAIN));
    }

    public void trackSearchSubmission(Screen screen, Optional<Urn> optionalQueryUrn) {
        if (optionalQueryUrn.isPresent()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.searchStart(screen,
                    new SearchQuerySourceInfo(optionalQueryUrn.get())));
        }
    }

    public void trackResultsScreenEvent(Screen screen) {
        if (screenQueryUrnMap.containsKey(screen)) {
            final Urn queryUrn = screenQueryUrnMap.get(screen);
            if (queryUrn != Urn.NOT_SET) {
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(screen.get(),
                        new SearchQuerySourceInfo(queryUrn)));
            }
        }
    }

    public void setQueryUrnForScreen(Screen screen, Optional<Urn> optionalQueryUrn) {
        if (screenQueryUrnMap.containsKey(screen) && optionalQueryUrn.isPresent()) {
            screenQueryUrnMap.put(screen, optionalQueryUrn.get());
        }
    }

    public void reset() {
        this.initializeScreenQueryUrnMap();
    }

    @VisibleForTesting
    Map<Screen, Urn> getScreenQueryUrnMap() {
        return Collections.unmodifiableMap(screenQueryUrnMap);
    }
}
