package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
class ChartsTracker {
    private final EventBus eventBus;
    private Map<ChartType, ScreenData> screenDataMap;

    @Inject
    ChartsTracker(EventBus eventBus) {
        this.eventBus = eventBus;
        initMap();
    }

    void chartDataLoaded(Urn queryUrn, ChartType chartType, ChartCategory chartCategory, Urn genreUrn) {
        if (screenDataMap.get(chartType).isPostponedEvent) {
            trackPage(genreUrn, chartCategory, chartType, queryUrn);
        }
        screenDataMap.put(chartType, new ScreenData(queryUrn, false));
    }

    void clearTracker() {
        initMap();
    }

    void chartPageSelected(Urn genre, ChartCategory chartCategory, ChartType chartType) {
        if (screenDataMap.get(chartType).isTrackingDataLoaded()) {
            trackPage(genre, chartCategory, chartType, screenDataMap.get(chartType).queryUrn);
        } else {
            screenDataMap.put(chartType, ScreenData.EVENT_POSTPONED);
        }
    }

    void genrePageSelected(final Screen screen) {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(screen));
    }

    private void trackPage(Urn genre, ChartCategory chartCategory, ChartType chartType, Urn queryUrn) {
        final Screen screen = getScreen(chartType, chartCategory);
        final String screenString = String.format(screen.get(), genre.getStringId());
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(screenString, queryUrn));
    }

    private void initMap() {
        this.screenDataMap = new HashMap<>(ChartType.values().length);
        for (ChartType chartType : ChartType.values()) {
            this.screenDataMap.put(chartType, ScreenData.EMPTY);
        }
    }

    private Screen getScreen(ChartType type, ChartCategory category) {
        if (type == ChartType.TOP) {
            if (category == ChartCategory.MUSIC) {
                return Screen.MUSIC_TOP_50;
            } else if (category == ChartCategory.AUDIO) {
                return Screen.AUDIO_TOP_50;
            }
        } else if (type == ChartType.TRENDING) {
            if (category == ChartCategory.MUSIC) {
                return Screen.MUSIC_TRENDING;
            } else if (category == ChartCategory.AUDIO) {
                return Screen.AUDIO_TRENDING;
            }
        }
        throw new IllegalArgumentException("Unknown screen");
    }

    private static final class ScreenData {
        static final ScreenData EMPTY = new ScreenData(Urn.NOT_SET, false);
        static final ScreenData EVENT_POSTPONED = new ScreenData(Urn.NOT_SET, true);

        final Urn queryUrn;
        final boolean isPostponedEvent;

        private ScreenData(Urn queryUrn, boolean isPostponedEvent) {
            this.queryUrn = queryUrn;
            this.isPostponedEvent = isPostponedEvent;
        }

        private boolean isTrackingDataLoaded() {
            return queryUrn != Urn.NOT_SET;
        }
    }
}
