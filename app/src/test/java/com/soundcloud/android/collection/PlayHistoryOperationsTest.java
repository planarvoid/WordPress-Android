package com.soundcloud.android.collection;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;

public class PlayHistoryOperationsTest extends AndroidUnitTest {

    private static final List<TrackItem> TRACK_ITEMS = ModelFixtures.trackItems(10);
    private static final Urn URN_1 = Urn.forTrack(123L);
    private static final Urn URN_2 = Urn.forTrack(234L);
    private static final Urn URN_3 = Urn.forTrack(345L);
    private static final Urn URN_4 = Urn.forTrack(456L);

    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlayHistoryStorage playHistoryStorage;

    private Scheduler scheduler = Schedulers.immediate();

    private TestSubscriber<List<TrackItem>> subscriber;
    private PlayHistoryOperations operations;

    @Before
    public void setUp() throws Exception {
        when(playHistoryStorage.fetchPlayHistory(anyInt())).thenReturn(Observable.from(TRACK_ITEMS));
        subscriber = new TestSubscriber<>();
        operations = new PlayHistoryOperations(playbackInitiator, playHistoryStorage, scheduler);
    }

    @Test
    public void playHistoryWithLimitReturnsFromLocalStorage() throws Exception {
        operations.playHistory(3).subscribe(subscriber);

        subscriber.assertValue(TRACK_ITEMS.subList(0, 3));
        subscriber.assertCompleted();
    }

    @Test
    public void playHistoryDeduplicatesTracksByTrackId() throws Exception {
        List<TrackItem> withDuplicates = tracksFromUrns(URN_1, URN_3, URN_1, URN_3, URN_2, URN_4);
        List<TrackItem> withoutDuplicates = tracksFromUrns(URN_1, URN_3, URN_2, URN_4);

        when(playHistoryStorage.fetchPlayHistory(anyInt())).thenReturn(Observable.from(withDuplicates));

        operations.playHistory(3).subscribe(subscriber);

        subscriber.assertValue(withoutDuplicates.subList(0, 3));
        subscriber.assertCompleted();
    }

    @Test
    public void playHistoryReturnsAllTracksWhenNoLimitIsSpecified() throws Exception {
        operations.playHistory().subscribe(subscriber);

        subscriber.assertValue(TRACK_ITEMS);
        subscriber.assertCompleted();
    }

    private List<TrackItem> tracksFromUrns(Urn... urns) {
        return Lists.newArrayList(MoreCollections.transform(
                Arrays.asList(urns),
                new Function<Urn, TrackItem>() {
                    public TrackItem apply(Urn urn) {
                        return TrackItem.from(PropertySet.from(EntityProperty.URN.bind(urn)));
                    }
                }));
    }

}
