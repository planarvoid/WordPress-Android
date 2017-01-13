package com.soundcloud.android.tracks;

import static java.util.Arrays.asList;
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
import com.soundcloud.android.playlists.LoadPlaylistTracksCommand;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.utils.EntityUtils;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

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
    private TrackItem trackItem;
    private PropertySet trackDescription;

    private TrackItem trackItem1 = ModelFixtures.trackItem();
    private TrackItem trackItem2 = ModelFixtures.trackItem();

    @Mock private TrackStorage trackStorage;
    @Mock private LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    @Mock private AccountOperations accountOperations;
    @Mock private SyncInitiator syncInitiator;

    private TestSubscriber<PropertySet> propSubscriber = new TestSubscriber<>();
    private TestSubscriber<TrackItem> trackItemSubscriber = new TestSubscriber<>();
    private TestSubscriber<Map<Urn,TrackItem>> mapSubscriber = new TestSubscriber<>();
    private PublishSubject<SyncJobResult> syncTracksSubject = PublishSubject.create();

    @Before
    public void setUp() {
        trackRepository = new TrackRepository(trackStorage, loadPlaylistTracksCommand, syncInitiator, Schedulers.immediate());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        track = PropertySet.from(TrackProperty.URN.bind(trackUrn),
                                 PlayableProperty.TITLE.bind(TITLE),
                                 PlayableProperty.CREATOR_NAME.bind(CREATOR));
        trackItem = TestPropertySets.trackWith(track);
        trackDescription = PropertySet.from(TrackProperty.DESCRIPTION.bind(DESCRIPTION));
    }

    @Test
    public void tracksLoadAvailableTracksFromStorage() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = singletonList(trackUrn);
        final Map<Urn, TrackItem> syncedTrackProperties = singletonMap(trackUrn, trackItem);

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(Observable.just(availableTracks));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(Observable.just(syncedTrackProperties));

        trackRepository.track(trackUrn).subscribe(trackItemSubscriber);

        trackItemSubscriber.assertValue(trackItem);
        verifyNoMoreInteractions(syncInitiator);
    }

    @Test
    public void tracksSyncsMissingTracks() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = emptyList();
        final TrackItem syncedTrack = TestPropertySets.expectedTrackForPlayer();
        final Map<Urn, TrackItem> actualTrackProperties = singletonMap(trackUrn, syncedTrack);

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(Observable.just(availableTracks));
        when(syncInitiator.batchSyncTracks(requestedTracks)).thenReturn(Observable.just(getSuccessResult()));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(Observable.just(actualTrackProperties));

        trackRepository.track(trackUrn).subscribe(trackItemSubscriber);

        trackItemSubscriber.assertValue(syncedTrack);
    }

    @Test
    public void trackReturnsEmptyWhenLoadingFailed() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = emptyList();
        final Map<Urn, TrackItem> syncedTracks = emptyMap();

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(Observable.just(availableTracks));
        when(syncInitiator.batchSyncTracks(requestedTracks)).thenReturn(Observable.just(getSuccessResult()));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(Observable.just(syncedTracks));

        trackRepository.track(trackUrn).subscribe(trackItemSubscriber);

        trackItemSubscriber.assertValue(null);
    }

    @Test
    public void fullTrackWithUpdateReturnsTrackDetailsFromStorage() {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.empty());
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(propSubscriber);

        final TrackItem first = TestPropertySets.trackWith(propSubscriber.getOnNextEvents().get(0));
        assertThat(first.getTitle()).isEqualTo(trackItem.getTitle());
        assertThat(first.getCreatorName()).isEqualTo(trackItem.getCreatorName());
        assertThat(first.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    public void fullTrackWithUpdateEmitsTrackFromStorageTwice() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(getSuccessResult()));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(propSubscriber);

        final PropertySet propertySet = track.merge(trackDescription);
        assertThat(propSubscriber.getOnNextEvents()).containsExactly(propertySet, propertySet);
        verify(trackStorage, times(2)).loadTrack(trackUrn);
    }

    @Test
    public void fromUrnsLoadsTracksFromStorage() throws CreateModelException {
        List<Urn> urns = asList(trackItem1.getUrn(), trackItem2.getUrn());
        when(trackStorage.availableTracks(urns)).thenReturn(Observable.just(urns));
        when(trackStorage.loadTracks(urns))
                .thenReturn(Observable.just(EntityUtils.toEntityMap(asList(trackItem1, trackItem2))));


        trackRepository.fromUrns(urns).subscribe(mapSubscriber);

        mapSubscriber.assertValue(EntityUtils.toEntityMap(asList(trackItem1, trackItem2)));
        mapSubscriber.assertCompleted();
    }

    @Test
    public void fromUrnsLoadsTracksFromStorageAfterSyncingMissingTracks() throws CreateModelException {
        List<Urn> urns = asList(trackItem1.getUrn(), trackItem2.getUrn());
        when(trackStorage.availableTracks(urns)).thenReturn(Observable.just(singletonList(trackItem1.getUrn())));
        when(syncInitiator.batchSyncTracks(singletonList(trackItem2.getUrn()))).thenReturn(syncTracksSubject);
        when(trackStorage.loadTracks(urns))
                .thenReturn(Observable.just(EntityUtils.toEntityMap(asList(trackItem1, trackItem2))));

        trackRepository.fromUrns(urns).subscribe(mapSubscriber);

        mapSubscriber.assertNoValues();
        syncTracksSubject.onNext(TestSyncJobResults.successWithChange());
        syncTracksSubject.onCompleted();
        mapSubscriber.assertValue(EntityUtils.toEntityMap(asList(trackItem1, trackItem2)));
        mapSubscriber.assertCompleted();
    }

    private SyncJobResult getSuccessResult() {
        return SyncJobResult.success("action", true);
    }
}
