package com.soundcloud.android.tracks;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Map;

public class TrackRepositoryTest extends AndroidUnitTest {

    public static final String TITLE = "title";
    public static final String CREATOR = "creator";
    public static final String DESCRIPTION = "Description...";

    private TrackRepository trackRepository;

    private Urn trackUrn = Urn.forTrack(123L);
    private Urn userUrn = Urn.forUser(123L);
    private PropertySet track;
    private PropertySet trackDescription;

    @Mock private TrackStorage trackStorage;
    @Mock private AccountOperations accountOperations;
    @Mock private SyncInitiator syncInitiator;

    private TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() {
        trackRepository = new TrackRepository(trackStorage, syncInitiator, Schedulers.immediate());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        track = PropertySet.from(TrackProperty.URN.bind(trackUrn),
                                 PlayableProperty.TITLE.bind(TITLE),
                                 PlayableProperty.CREATOR_NAME.bind(CREATOR));

        trackDescription = PropertySet.from(TrackProperty.DESCRIPTION.bind(DESCRIPTION));
    }

    @Test
    public void tracksLoadAvailableTracksFromStorage() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = singletonList(trackUrn);
        final Map<Urn, PropertySet> syncedTrackProperties = singletonMap(trackUrn, track);

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(Observable.just(availableTracks));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(Observable.just(syncedTrackProperties));

        trackRepository.track(trackUrn).subscribe(subscriber);

        subscriber.assertValue(track);
        verifyNoMoreInteractions(syncInitiator);
    }

    @Test
    public void tracksSyncsMissingTracks() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = emptyList();
        final PropertySet syncedTrack = TestPropertySets.expectedTrackForPlayer();
        final Map<Urn, PropertySet> actualTrackProperties = singletonMap(trackUrn, syncedTrack);

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(Observable.just(availableTracks));
        when(syncInitiator.batchSyncTracks(requestedTracks)).thenReturn(Observable.just(getSuccessResult()));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(Observable.just(actualTrackProperties));

        trackRepository.track(trackUrn).subscribe(subscriber);

        subscriber.assertValue(syncedTrack);
    }

    @Test
    public void trackReturnsEmptyWhenLoadingFailed() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = emptyList();
        final Map<Urn, PropertySet> syncedTracks = emptyMap();

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(Observable.just(availableTracks));
        when(syncInitiator.batchSyncTracks(requestedTracks)).thenReturn(Observable.just(getSuccessResult()));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(Observable.just(syncedTracks));

        trackRepository.track(trackUrn).subscribe(subscriber);

        subscriber.assertValue(PropertySet.create());
    }

    @Test
    public void fullTrackWithUpdateReturnsTrackDetailsFromStorage() {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.<SyncJobResult>empty());
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(subscriber);

        final PropertySet first = subscriber.getOnNextEvents().get(0);
        assertThat(first.get(PlayableProperty.TITLE)).isEqualTo(TITLE);
        assertThat(first.get(PlayableProperty.CREATOR_NAME)).isEqualTo(CREATOR);
        assertThat(first.get(TrackProperty.DESCRIPTION)).isEqualTo(DESCRIPTION);
    }

    @Test
    public void fullTrackWithUpdateEmitsTrackFromStorageTwice() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(getSuccessResult()));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(subscriber);

        final PropertySet propertySet = track.merge(trackDescription);
        assertThat(subscriber.getOnNextEvents()).containsExactly(propertySet, propertySet);
        verify(trackStorage, times(2)).loadTrack(trackUrn);
    }

    private SyncJobResult getSuccessResult() {
        return SyncJobResult.success("action", true);
    }
}
