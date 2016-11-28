package com.soundcloud.android.playback;

import static com.soundcloud.android.events.CurrentPlayQueueItemEvent.fromNewQueue;
import static com.soundcloud.android.events.CurrentPlayQueueItemEvent.fromPositionChanged;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.SharedPreferences;

import java.io.IOException;
import java.util.List;

public class PlayQueueExtenderTest extends AndroidUnitTest {

    @Mock private StationsOperations stationsOperations;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private FeatureFlags featureFlags;

    private final Urn LAST_URN = Urn.forTrack(987);
    private final Urn TRACK_URN = Urn.forTrack(123);
    private final PlayQueueItem trackPlayQueueItem = TestPlayQueueItem.createTrack(TRACK_URN);
    private final PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
    private final PlayQueue recommendedPlayQueue = TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L),
                                                                          playSessionSource);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(playQueueManager.getLastPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(LAST_URN));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(playSessionSource);
        when(playQueueManager.getUpcomingPlayQueueItems(anyInt())).thenReturn(Lists.<Urn>newArrayList());
        when(sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true)).thenReturn(true);
        when(playQueueOperations.relatedTracksPlayQueue(eq(LAST_URN), anyBoolean(), any(PlaySessionSource.class)))
                .thenReturn(Observable.just(recommendedPlayQueue));

        PlayQueueExtender extender = new PlayQueueExtender(playQueueManager,
                                                           playQueueOperations,
                                                           stationsOperations,
                                                           sharedPreferences,
                                                           eventBus,
                                                           featureFlags);
        extender.subscribe();
    }

    @Test
    public void appendsRecommendedTracksWhenAtTolerance() {
        when(playQueueManager.getQueueSize()).thenReturn(PlayQueueExtender.RECOMMENDED_LOAD_TOLERANCE);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEnd() {
        setWithinToleranceAtEnd();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsStationsTracksWhenAtTheEndOfAStationsPlayQueue() {
        final Urn station = Urn.forTrackStation(123L);
        final List<StationTrack> stationTracks = singletonList(StationTrack.create(TRACK_URN, Urn.NOT_SET));
        final PlayQueue playQueue = PlayQueue.fromStation(station, stationTracks, playSessionSource);
        final int queueSize = PlayQueueExtender.RECOMMENDED_LOAD_TOLERANCE;

        when(playQueueManager.getQueueSize()).thenReturn(queueSize);
        when(playQueueManager.getCollectionUrn()).thenReturn(station);
        when(stationsOperations.fetchUpcomingTracks(station, queueSize, playSessionSource))
                .thenReturn(Observable.just(playQueue));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, station, 0));

        verify(playQueueManager).appendPlayQueueItems(playQueue);
    }

    @Test
    public void doesNotAppendsRecommendedTracksWhenAtEndIfPreferenceOff() {
        when(sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true)).thenReturn(false);
        setWithinToleranceAtEnd();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForExplore() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.EXPLORE_AUDIO_GENRE));
        setWithinToleranceAtEnd();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForDeeplinks() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.DEEPLINK));
        setWithinToleranceAtEnd();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksWhenAtEndForSearchSuggestions() {
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource(Screen.SEARCH_SUGGESTIONS));
        setWithinToleranceAtEnd();

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void doesNotAppendRecommendedTracksWhenQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void doesNotAppendRecommendedTracksMoreThanTolerance() {
        when(playQueueManager.getPlayableQueueItemsRemaining()).thenReturn(PlayQueueExtender.RECOMMENDED_LOAD_TOLERANCE + 1);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verifyZeroInteractions(playQueueOperations);
    }

    @Test
    public void retriesToAppendRecommendedTracksAfterError() {
        setWithinToleranceAtEnd();
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true, playQueueManager.getCurrentPlaySessionSource()))
                .thenReturn(Observable.<PlayQueue>error(new IOException()))
                .thenReturn(Observable.just(recommendedPlayQueue));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void appendsRecommendedTracksConsecutivelyIfResultsAreReceivedFirstTime() {
        final PlayQueue playQueue = TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L), playSessionSource);
        setWithinToleranceAtEnd();
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true, playSessionSource))
                .thenReturn(Observable.just(playQueue))
                .thenReturn(Observable.just(recommendedPlayQueue));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    @Test
    public void unsubscribesFromRecommendedTracksLoadWhenQueueChangesAndLoadsNewRecommendations() {
        final PublishSubject<PlayQueue> recommendedSubject = PublishSubject.create();
        final PublishSubject<PlayQueue> secondRecommendedSubject = PublishSubject.create();

        when(playQueueOperations.relatedTracksPlayQueue(LAST_URN, true, playSessionSource))
                .thenReturn(recommendedSubject)
                .thenReturn(secondRecommendedSubject);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));
        assertThat(recommendedSubject.hasObservers()).isTrue();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));
        assertThat(recommendedSubject.hasObservers()).isFalse();
        assertThat(secondRecommendedSubject.hasObservers()).isTrue();
    }

    @Test
    public void appendsRecommendedTracksWhenPlayQueueFeatureIsEnabledRegardlessOfUserPreference() {
        when(featureFlags.isEnabled(Flag.PLAY_QUEUE)).thenReturn(true);
        when(sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true)).thenReturn(false);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).appendPlayQueueItems(recommendedPlayQueue);
    }

    private void setWithinToleranceAtEnd() {
        when(playQueueManager.getQueueSize()).thenReturn(PlayQueueExtender.RECOMMENDED_LOAD_TOLERANCE);
        when(playQueueManager.getCurrentPosition()).thenReturn(PlayQueueExtender.RECOMMENDED_LOAD_TOLERANCE - 1);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
    }

}
