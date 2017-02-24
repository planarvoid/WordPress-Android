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
import static rx.Observable.just;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTracksCommand;
import com.soundcloud.android.sync.EntitySyncStateStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TrackRepositoryTest extends AndroidUnitTest {

    public static final String TITLE = "title";
    private static final String CREATOR = "creator";
    private static final String DESCRIPTION = "Description...";
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(1);

    private TrackRepository trackRepository;

    private Urn trackUrn = Urn.forTrack(123L);
    private Urn userUrn = Urn.forUser(123L);
    private Track track;
    private TrackItem trackItem;

    private Track track1 = ModelFixtures.trackBuilder().build();
    private Track track2 = ModelFixtures.trackBuilder().build();

    @Mock private TrackStorage trackStorage;
    @Mock private LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    @Mock private AccountOperations accountOperations;
    @Mock private SyncInitiator syncInitiator;
    @Mock private EntitySyncStateStorage entitySyncStateStorage;

    private TestSubscriber<Track> propSubscriber = new TestSubscriber<>();
    private TestSubscriber<Track> trackItemSubscriber = new TestSubscriber<>();
    private TestSubscriber<Map<Urn,Track>> mapSubscriber = new TestSubscriber<>();
    private PublishSubject<SyncJobResult> syncSubject = PublishSubject.create();
    private TestDateProvider currentTimeProvider = new TestDateProvider();

    @Before
    public void setUp() {
        trackRepository = new TrackRepository(trackStorage, loadPlaylistTracksCommand, syncInitiator, Schedulers.immediate(), entitySyncStateStorage, currentTimeProvider);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        track = ModelFixtures.trackBuilder().urn(trackUrn).title(TITLE).creatorName(CREATOR).build();
        trackItem = TrackItem.from(track);
    }

    @Test
    public void tracksLoadAvailableTracksFromStorage() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = singletonList(trackUrn);
        final Map<Urn, Track> syncedTrackProperties = singletonMap(trackUrn, track);

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(just(availableTracks));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(just(syncedTrackProperties));

        trackRepository.track(trackUrn).subscribe(trackItemSubscriber);

        trackItemSubscriber.assertValue(track);
        verifyNoMoreInteractions(syncInitiator);
    }

    @Test
    public void tracksSyncsMissingTracks() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = emptyList();
        final Track syncedTrack = ModelFixtures.trackBuilder().build();
        final Map<Urn, Track> actualTrackProperties = singletonMap(trackUrn, syncedTrack);

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(just(availableTracks));
        when(syncInitiator.batchSyncTracks(requestedTracks)).thenReturn(just(getSuccessResult()));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(just(actualTrackProperties));

        trackRepository.track(trackUrn).subscribe(trackItemSubscriber);

        trackItemSubscriber.assertValue(syncedTrack);
    }

    @Test
    public void trackReturnsEmptyWhenLoadingFailed() {
        final List<Urn> requestedTracks = singletonList(trackUrn);
        final List<Urn> availableTracks = emptyList();
        final Map<Urn, Track> syncedTracks = emptyMap();

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(just(availableTracks));
        when(syncInitiator.batchSyncTracks(requestedTracks)).thenReturn(just(getSuccessResult()));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(just(syncedTracks));

        trackRepository.track(trackUrn).subscribe(trackItemSubscriber);

        trackItemSubscriber.assertNoValues();
    }

    @Test
    public void fullTrackWithUpdateReturnsTrackDetailsFromStorage() {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.empty());
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(Optional.of(track)));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(Optional.of(DESCRIPTION)));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(propSubscriber);

        final Track first = propSubscriber.getOnNextEvents().get(0);
        assertThat(first.title()).isEqualTo(trackItem.title());
        assertThat(first.creatorName()).isEqualTo(trackItem.creatorName());
        assertThat(first.description().get()).isEqualTo(DESCRIPTION);
    }

    @Test
    public void fullTrackWithUpdateEmitsTrackFromStorageTwice() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(getSuccessResult()));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(Optional.of(track)));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(Optional.of(DESCRIPTION)));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(propSubscriber);

        final Track propertySet = Track.builder(track).description(Optional.of(DESCRIPTION)).build();
        assertThat(propSubscriber.getOnNextEvents()).containsExactly(propertySet, propertySet);
        verify(trackStorage, times(2)).loadTrack(trackUrn);
    }

    @Test
    public void fromUrnsLoadsTracksFromStorage() throws CreateModelException {
        List<Urn> urns = asList(track1.urn(), track2.urn());
        when(trackStorage.availableTracks(urns)).thenReturn(Observable.just(urns));
        final List<Track> trackList = asList(track1, track2);
        when(trackStorage.loadTracks(urns))
                .thenReturn(Observable.just(toMap(trackList)));


        trackRepository.fromUrns(urns).subscribe(mapSubscriber);

        mapSubscriber.assertValue(toMap(trackList));
        mapSubscriber.assertCompleted();
    }

    @Test
    public void fromUrnsLoadsTracksFromStorageAfterSyncingMissingTracks() throws CreateModelException {
        List<Urn> urns = asList(track1.urn(), track2.urn());
        when(trackStorage.availableTracks(urns)).thenReturn(Observable.just(singletonList(track1.urn())));
        when(syncInitiator.batchSyncTracks(singletonList(track2.urn()))).thenReturn(syncSubject);
        when(trackStorage.loadTracks(urns))
                .thenReturn(Observable.just(toMap(asList(track1, track2))));

        trackRepository.fromUrns(urns).subscribe(mapSubscriber);

        mapSubscriber.assertNoValues();
        syncSubject.onNext(TestSyncJobResults.successWithChange());
        syncSubject.onCompleted();
        mapSubscriber.assertValue(toMap(asList(track1, track2)));
        mapSubscriber.assertCompleted();
    }

    @Test
    public void fromPlaylistBackfillsIfNeverSynced() {
        when(entitySyncStateStorage.hasSyncedBefore(PLAYLIST_URN)).thenReturn(false);
        
        final List<Track> trackList = singletonList(track);


        when(syncInitiator.syncPlaylist(PLAYLIST_URN)).thenReturn(syncSubject);
        when(loadPlaylistTracksCommand.toObservable(PLAYLIST_URN)).thenReturn(just(trackList));

        AssertableSubscriber<List<Track>> testSubscriber = trackRepository.forPlaylist(PLAYLIST_URN).test();
        testSubscriber.assertNoValues();

        syncSubject.onNext(TestSyncJobResults.successWithChange());
        syncSubject.onCompleted();
        testSubscriber.assertValue(trackList)
                       .assertCompleted();
    }

    @Test
    public void fromPlaylistDoesNotBackfillIfSynced() {
        when(entitySyncStateStorage.hasSyncedBefore(PLAYLIST_URN)).thenReturn(true);

        final List<Track> trackList = singletonList(track);

        when(syncInitiator.syncPlaylist(PLAYLIST_URN)).thenReturn(syncSubject);
        when(loadPlaylistTracksCommand.toObservable(PLAYLIST_URN)).thenReturn(just(trackList));

        AssertableSubscriber<List<Track>> testSubscriber = trackRepository.forPlaylist(PLAYLIST_URN).test();
        testSubscriber.assertValue(trackList)
                       .assertCompleted();
    }

    @Test
    public void fromPlaylistWithStaleTimeBackfillsIfStaleTimePassed() throws Exception {
        when(entitySyncStateStorage.lastSyncTime(PLAYLIST_URN)).thenReturn(1000L);
        currentTimeProvider.setTime(2, TimeUnit.SECONDS);

        final List<Track> trackList = singletonList(track);

        when(syncInitiator.syncPlaylist(PLAYLIST_URN)).thenReturn(syncSubject);
        when(loadPlaylistTracksCommand.toObservable(PLAYLIST_URN)).thenReturn(just(trackList));

        AssertableSubscriber<List<Track>> testSubscriber = trackRepository.forPlaylist(PLAYLIST_URN, 999).test();
        testSubscriber.assertNoValues();

        syncSubject.onNext(TestSyncJobResults.successWithChange());
        syncSubject.onCompleted();
        testSubscriber.assertValue(trackList)
                      .assertCompleted();

    }

    @Test
    public void fromPlaylistDoesNotBackfillIfBeforeStaleTime() {
        when(entitySyncStateStorage.lastSyncTime(PLAYLIST_URN)).thenReturn(1000L);
        currentTimeProvider.setTime(2, TimeUnit.SECONDS);

        final List<Track> trackList = singletonList(track);

        when(syncInitiator.syncPlaylist(PLAYLIST_URN)).thenReturn(syncSubject);
        when(loadPlaylistTracksCommand.toObservable(PLAYLIST_URN)).thenReturn(just(trackList));

        AssertableSubscriber<List<Track>> testSubscriber = trackRepository.forPlaylist(PLAYLIST_URN, 1001).test();
        testSubscriber.assertValue(trackList)
                      .assertCompleted();
    }

    @Test
    public void tracklistFromUrnsReturnsListWithoutUnavailableTracks() throws Exception {
        final List<Urn> requestedTracks = asList(track1.urn(), track2.urn());
        final List<Urn> availableTracks = singletonList(track1.urn());
        // omit track 2, as if it were unavailable
        final Map<Urn, Track> actualTrackProperties = singletonMap(track1.urn(), track1);

        when(trackStorage.availableTracks(requestedTracks)).thenReturn(just(availableTracks));
        when(syncInitiator.batchSyncTracks(singletonList(track2.urn()))).thenReturn(just(getSuccessResult()));
        when(trackStorage.loadTracks(requestedTracks)).thenReturn(just(actualTrackProperties));

        trackRepository.trackListFromUrns(requestedTracks).test().assertValue(
                singletonList(track1)
        );
    }

    private Map<Urn, Track> toMap(List<Track> tracks) {
        final Map<Urn, Track> result = new HashMap<>(tracks.size());
        for (Track t : tracks) {
            result.put(t.urn(), t);
        }
        return result;
    }

    private SyncJobResult getSuccessResult() {
        return SyncJobResult.success("action", true);
    }
}
