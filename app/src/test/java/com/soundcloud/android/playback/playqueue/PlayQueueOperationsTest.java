package com.soundcloud.android.playback.playqueue;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueStorage;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.functions.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayQueueOperationsTest extends AndroidUnitTest {

    private PlayQueueOperations operations;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackRepository trackRepository;
    @Mock private PlayQueueStorage storage;
    @Captor private ArgumentCaptor<Predicate<PlayQueueItem>> predicateCaptor;

    private TestSubscriber<List<TrackAndPlayQueueItem>> subscriber = new TestSubscriber<>();

    private final Track track1 = Track.from(ModelFixtures.create(ApiTrack.class));
    private final Track track2 = Track.from(ModelFixtures.create(ApiTrack.class));
    private final TrackItem trackItem1 = TrackItem.from(track1);
    private final TrackItem trackItem2 = TrackItem.from(track2);
    private final Urn track1Urn = trackItem1.getUrn();
    private final Urn track2Urn = trackItem2.getUrn();
    private final TrackQueueItem trackQueueItem1 = trackQueueItem(track1Urn);
    private final TrackQueueItem trackQueueItem2 = trackQueueItem(track2Urn);
    private final TrackAndPlayQueueItem trackAndPlayQueueItem1 = new TrackAndPlayQueueItem(trackItem1, trackQueueItem1);
    private final TrackAndPlayQueueItem trackAndPlayQueueItem2 = new TrackAndPlayQueueItem(trackItem2, trackQueueItem2);

    @Before
    public void setUp() throws Exception {
        operations = new PlayQueueOperations(Schedulers.immediate(),
                                             playQueueManager,
                                             trackRepository,
                                             storage);
    }

    @Test
    public void getTrackItemsReturnsTrackItemsFromPlayQueue() {
        final List<PlayQueueItem> playQueue = asList(trackQueueItem1, trackQueueItem2);
        final Map<Urn, Track> tracksFromStorage = new HashMap<>();
        tracksFromStorage.put(track1Urn, track1);
        tracksFromStorage.put(track2Urn, track2);
        final List<TrackAndPlayQueueItem> expected = asList(trackAndPlayQueueItem1, trackAndPlayQueueItem2);

        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(playQueue);
        when(trackRepository.fromUrns(asList(track1Urn, track2Urn))).thenReturn(just(tracksFromStorage));

        operations.getTracks().subscribe(subscriber);

        subscriber.assertValue(expected);
        subscriber.assertTerminalEvent();
    }

    @Test
    public void getTrackItemsDeferPlayQueueItemsLoadingToTheSubscription() {
        when(trackRepository.fromUrns(singletonList(track1Urn))).thenReturn(just(singletonMap(track1Urn, track1)));
        when(trackRepository.fromUrns(singletonList(track2Urn))).thenReturn(just(singletonMap(track2Urn, track2)));

        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(singletonList(trackQueueItem1));
        final Observable<List<TrackAndPlayQueueItem>> operation = operations.getTracks();
        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(singletonList(trackQueueItem2));

        operation.subscribe(subscriber);

        for (Throwable throwable : subscriber.getOnErrorEvents()) {
            throwable.printStackTrace();
        }

        subscriber.assertValue(singletonList(trackAndPlayQueueItem2));
        subscriber.assertTerminalEvent();
    }

    @Test
    public void getTrackItemsFiltersOutNonTracksFromPlayQueueManager() {
        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(Collections.emptyList());

        operations.getTracks().subscribe(subscriber);

        verify(playQueueManager).getPlayQueueItems(predicateCaptor.capture());

        final Predicate<PlayQueueItem> predicate = predicateCaptor.getValue();
        assertThat(predicate.apply(TestPlayQueueItem.createTrack(Urn.forTrack(1)))).isTrue();
        assertThat(predicate.apply(TestPlayQueueItem.createPlaylist(Urn.forPlaylist(1)))).isFalse();
        assertThat(predicate.apply(TestPlayQueueItem.createTrack(Urn.forAd("dfs:ads", "something")))).isFalse();
        assertThat(predicate.apply(null)).isFalse();
    }

    @Test
    public void getTrackItemsFiltersUnknownTracks() {
        final List<Urn> requestedTracks = asList(track1Urn, track2Urn);
        final List<PlayQueueItem> playQueueItems = asList(trackQueueItem1, trackQueueItem2);
        final Map<Urn, Track> knownTrack = singletonMap(track1Urn, track1);
        final List<TrackAndPlayQueueItem> expectedTrackItems = singletonList(trackAndPlayQueueItem1);

        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(playQueueItems);
        when(trackRepository.fromUrns(requestedTracks)).thenReturn(just(knownTrack));

        operations.getTracks().subscribe(subscriber);

        subscriber.assertValue(expectedTrackItems);
    }

    public TrackQueueItem trackQueueItem(Urn urn) {
        return TestPlayQueueItem.createTrack(urn);
    }
}
