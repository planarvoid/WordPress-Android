package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayQueueOperationsTest extends AndroidUnitTest {

    private PlayQueueOperations operations;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackRepository trackRepository;
    @Mock private LoadTrackImageResource loadTrackImageResource;
    @Captor private ArgumentCaptor<Predicate<PlayQueueItem>> predicateCaptor;

    private TestSubscriber<List<TrackItem>> subscriber = new TestSubscriber<>();

    final PropertySet track1 = TestPropertySets.fromApiTrack();
    final PropertySet track2 = TestPropertySets.fromApiTrack();
    final Urn track1Urn = track1.get(TrackProperty.URN);
    final Urn track2Urn = track2.get(TrackProperty.URN);

    @Before
    public void setUp() throws Exception {
        operations = new PlayQueueOperations(Schedulers.immediate(), playQueueManager, trackRepository, loadTrackImageResource);
    }

    @Test
    public void getTrackItemsReturnsTrackItemsFromPlayQueue() {
        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(getTrackQueue(track1Urn, track2Urn));
        when(trackRepository.track(track1Urn)).thenReturn(Observable.just(track1));
        when(trackRepository.track(track2Urn)).thenReturn(Observable.just(track2));

        operations.getTrackItems().subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(Arrays.asList(TrackItem.from(track1), TrackItem.from(track2))));
        subscriber.assertTerminalEvent();
    }

    @Test
    public void getItemsFiltersOutNonTracksFromPlayQueueManager() {
        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(Collections.emptyList());

        operations.getTrackItems().subscribe(subscriber);

        verify(playQueueManager).getPlayQueueItems(predicateCaptor.capture());

        final Predicate<PlayQueueItem> predicate = predicateCaptor.getValue();
        assertThat(predicate.apply(TestPlayQueueItem.createTrack(Urn.forTrack(1)))).isTrue();
        assertThat(predicate.apply(TestPlayQueueItem.createPlaylist(Urn.forPlaylist(1)))).isFalse();
    }

    @NonNull
    public List<TrackQueueItem> getTrackQueue(Urn track1Urn, Urn track2Urn) {
        return Arrays.asList(
                getTrackItem(track1Urn),
                getTrackItem(track2Urn)
        );
    }

    public TrackQueueItem getTrackItem(Urn urn) {
        return TestPlayQueueItem.createTrack(
                urn);
    }
}
