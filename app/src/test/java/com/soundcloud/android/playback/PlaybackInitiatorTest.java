package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.TestUrns.createTrackUrns;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.Station;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public class PlaybackInitiatorTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);
    private static final String EXPLORE_VERSION = "explore-version";
    private static final Screen ORIGIN_SCREEN = Screen.EXPLORE_TRENDING_MUSIC;

    private PlaybackInitiator playbackInitiator;

    private PublicApiPlaylist playlist;

    @Mock private TrackStorage trackStorage;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlaySessionController playSessionController;

    @Captor private ArgumentCaptor<PlayQueue> playQueueTracksCaptor;

    private TestObserver<PlaybackResult> observer;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setUp() throws Exception {

        playbackInitiator = new PlaybackInitiator(
                trackStorage,
                playQueueManager,
                playQueueOperations,
                playSessionController);

        playlist = ModelFixtures.create(PublicApiPlaylist.class);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK1);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());

        observer = new TestObserver<>();
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, new Urn("soundcloud:tracks:1"));
    }

    @Test
     public void playTrackPlaysNewQueueFromInitialTrack() {
        playbackInitiator.playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN)).subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        verify(playSessionController).playNewQueue(createPlayQueueFromUrns(playSessionSource, TRACK1), TRACK1, 0, false, playSessionSource);
    }

    @Test
    public void playTrackShouldNotSendServiceIntentIfTrackAlreadyPlayingWithSameOrigin() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        playbackInitiator.playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN));

        verifyZeroInteractions(playSessionController);
    }

    @Test
    public void playTrackShouldPlayNewQueueIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());

        PlaySessionSource newPlaySessionSource = new PlaySessionSource(Screen.EXPLORE_TRENDING_AUDIO);
        playbackInitiator.playTracks(
                Observable.just(TRACK1).toList(), TRACK1, 0, newPlaySessionSource)
                .subscribe(observer);

        verify(playSessionController).playNewQueue(createPlayQueueFromUrns(newPlaySessionSource, TRACK1), TRACK1, 0, false, newPlaySessionSource);
    }

    @Test
    public void playPostsPlaysNewQueueFromInitialTrack() {
        final PropertySet track = PropertySet.from(TrackProperty.URN.bind(TRACK1));
        playbackInitiator.playPosts(Observable.just(track).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN)).subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        verify(playSessionController).playNewQueue(createPlayQueue(playSessionSource, track), TRACK1, 0, false, playSessionSource);
    }

    @Test
    public void playPostsShouldNotSendServiceIntentIfTrackAlreadyPlayingWithSameOriginAndCollectionUrn() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        final PropertySet track = PropertySet.from(TrackProperty.URN.bind(TRACK1));
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN);
        when(playQueueManager.isCurrentCollection(playSessionSource.getCollectionUrn())).thenReturn(true);
        playbackInitiator.playPosts(Observable.just(track).toList(), TRACK1, 0, playSessionSource).subscribe(observer);

        verifyZeroInteractions(playSessionController);
    }

    @Test
    public void playPostsShouldPlayNewQueueIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());

        PlaySessionSource newPlaySessionSource = new PlaySessionSource(Screen.EXPLORE_TRENDING_AUDIO);
        final PropertySet track = PropertySet.from(TrackProperty.URN.bind(TRACK1));
        playbackInitiator.playPosts(Observable.just(track).toList(), TRACK1, 0, newPlaySessionSource).subscribe(observer);

        verify(playSessionController).playNewQueue(createPlayQueue(newPlaySessionSource, track), TRACK1, 0, false, newPlaySessionSource);
    }

    @Test
    public void playExploreTrackPlaysNewQueue() {
        final PlaySessionSource playSessionSource = PlaySessionSource.forExplore(ORIGIN_SCREEN.get(), EXPLORE_VERSION);

        playbackInitiator.playTrackWithRecommendationsLegacy(TRACK1, playSessionSource).subscribe(observer);

        final PlaySessionSource expected = PlaySessionSource.forExplore(ORIGIN_SCREEN.get(), EXPLORE_VERSION);

        verify(playSessionController).playNewQueue(eq(createPlayQueueFromUrns(expected, TRACK1)), eq(TRACK1), eq(0), anyBoolean(), eq(expected));
    }

    @Test
    public void playExploreTrackPlaysNewQueueWithRelatedTracks() {
        playbackInitiator.playTrackWithRecommendationsLegacy(TRACK1, new PlaySessionSource(ORIGIN_SCREEN.get())).subscribe(observer);

        verify(playSessionController).playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), eq(true), any(PlaySessionSource.class));
    }

    @Test
    public void playTrackWithRecommendationsReturnsAnErrorWhenNoRecommendation() {
        when(playQueueOperations.relatedTracksPlayQueueWithSeedTrack(TRACK1)).thenReturn(Observable.<PlayQueue>error(new NoSuchElementException()));

        playbackInitiator.playTrackWithRecommendations(TRACK1, new PlaySessionSource(ORIGIN_SCREEN), 0).subscribe(observer);

        assertThat(observer.getOnErrorEvents()).hasSize(1);
    }

    @Test
    public void playTrackWithRecommendationsPlaysQueueWithSeedAtSpecifiedPosition() {
        final RecommendedTracksCollection relatedTracks = new RecommendedTracksCollection(Arrays.asList(new ApiTrack()), "");
        final PlayQueue relatedUrns = PlayQueue.fromRecommendationsWithPrependedSeed(TRACK1, relatedTracks);
        when(playQueueOperations.relatedTracksPlayQueueWithSeedTrack(TRACK1)).thenReturn(Observable.just(relatedUrns));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN);
        playbackInitiator.playTrackWithRecommendations(TRACK1, playSessionSource, 0).subscribe(observer);

        final PlayQueue expectedQueue = PlayQueue.fromRecommendationsWithPrependedSeed(TRACK1, relatedTracks);
        verify(playSessionController).playNewQueue(eq(expectedQueue), any(Urn.class), anyInt(), eq(false), any(PlaySessionSource.class));
    }

    @Test
    public void playFromPlaylistPlaysNewQueue() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);

        final List<Urn> trackUrns = createTrackUrns(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(ORIGIN_SCREEN.get(), playlist.getUrn(), playlist.getUserUrn(), playlist.getTrackCount());

        playbackInitiator
                .playTracks(Observable.just(trackUrns), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe(observer);

        final PlayQueue expectedQueue = createPlayQueueFromUrns(playSessionSource, tracks.get(0).getUrn(), tracks.get(1).getUrn(), tracks.get(2).getUrn());
        verify(playSessionController).playNewQueue(expectedQueue, tracks.get(1).getUrn(), 1, false, playSessionSource);
    }

    @Test
    public void playsNewQueueIfPlayingQueueHasSameContextWithDifferentPlaylistSources() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);

        final List<Urn> trackUrns = createTrackUrns(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get()); // same screen origin
        when(playQueueManager.isCurrentCollection(Urn.forPlaylist(1234))).thenReturn(false); // different Playlist Id
        when(playSessionController.playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));

        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(Screen.EXPLORE_TRENDING_MUSIC.get(), playlist.getUrn(), playlist.getUserUrn(), playlist.getTrackCount());

        playbackInitiator
                .playTracks(Observable.just(trackUrns), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        final PlayQueue expectedQueue = PlayQueue.fromTrackUrnList(trackUrns, playSessionSource);
        verify(playSessionController).playNewQueue(expectedQueue, tracks.get(1).getUrn(), 1, false, playSessionSource);
    }

    @Test
    public void playTracksNoOpsWhenItIsPQMCurrentTrackAndCurrentScreenSourceAndSameCollectionUrn()  {
        final String screen = "origin_screen";
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        when(playQueueManager.getScreenTag()).thenReturn(screen);
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.isCurrentCollection(Urn.NOT_SET)).thenReturn(true);

        playbackInitiator
                .playTracks(Observable.just(TRACK1).toList(), TRACK1, 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        verify(playSessionController, never()).playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class));
    }

    @Test
    public void playTracksNoOpsWhenItIsPQMCurrentTrackAndCurrentScreenSourceAndCurrentPlaylist()  {
        final Urn playlistUrn = Urn.forPlaylist(456L);
        final Urn playlistOwnerUrn = Urn.forUser(789L);
        final String screen = "origin_screen";
        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(screen, playlistUrn, playlistOwnerUrn, playlist.getTrackCount());
        when(playQueueManager.getScreenTag()).thenReturn(screen);
        when(playQueueManager.isCurrentCollection(playlistUrn)).thenReturn(true);
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.isCurrentCollection(playlistUrn)).thenReturn(true);

        playbackInitiator
                .playTracks(Observable.just(TRACK1).toList(), TRACK1, 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        verify(playSessionController, never()).playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class));
    }

    @Test
    public void playFromShuffledWithTracksObservablePlaysNewQueueWithGivenTrackIdList() {
        final List<Urn> tracksToPlay = Arrays.asList(TRACK1, TRACK2, TRACK3);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.YOUR_LIKES);

        playbackInitiator.playTracksShuffled(Observable.just(tracksToPlay), playSessionSource).subscribe(observer);

        verify(playSessionController).playNewQueue(playQueueTracksCaptor.capture(), any(Urn.class), eq(0), anyBoolean(), eq(playSessionSource));
        final PlayQueue actualQueue = playQueueTracksCaptor.getValue();
        assertThat(toList(actualQueue)).contains(TRACK1, TRACK2, TRACK3);
    }

    @Test
    public void playTracksShuffledDoesNotLoadRecommendations() {
        final List<Urn> idsOrig = createTrackUrns(1L, 2L, 3L);
        playbackInitiator.playTracksShuffled(Observable.just(idsOrig), new PlaySessionSource(Screen.YOUR_LIKES)).subscribe(observer);

        verify(playSessionController).playNewQueue(any(PlayQueue.class), any(Urn.class), eq(0), eq(false), any(PlaySessionSource.class));
    }

    @Test
    public void playTracksWithNonEmptyTrackListPlaysNewQueue() {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN);
        final List<Urn> tracksToPlay = Arrays.asList(TRACK1, TRACK2, TRACK3);
        playbackInitiator
                .playTracks(Observable.just(tracksToPlay), TRACK3, 2, playSessionSource)
                .subscribe(observer);

        final PlayQueue expectedPlayQueue = createPlayQueueFromUrns(playSessionSource, TRACK1, TRACK2, TRACK3);
        verify(playSessionController).playNewQueue(expectedPlayQueue, TRACK3, 2, false, playSessionSource);
    }

    @Test
    public void playTracksWithEmptyTrackListDoesNotPlayNewQueue() {
        playbackInitiator
                .playTracks(Observable.<Urn>empty().toList(), TRACK1, 2, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe(observer);

        verify(playSessionController, never()).playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), eq(false), any(PlaySessionSource.class));
    }

    @Test
    public void playFromAdapterPlaysNewQueueFromListOfTracks() throws Exception {
        List<Urn> playables = createTrackUrns(1L, 2L);

        playbackInitiator.playTracks(playables, 1, new PlaySessionSource(ORIGIN_SCREEN)).subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        verify(playSessionController).playNewQueue(createPlayQueueFromUrns(playSessionSource, Urn.forTrack(1L), Urn.forTrack(2L)), Urn.forTrack(2L), 1, false, playSessionSource);
    }

    @Test
    public void startPlaybackWithRecommendationsPlaysNewQueue() {
        playbackInitiator.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo).subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        verify(playSessionController).playNewQueue(
                eq(createPlayQueueFromUrns(playSessionSource, TRACK1)),
                eq(TRACK1),
                eq(0),
                anyBoolean(),
                eq(playSessionSource));
    }

    @Test
    public void startPlaybackWithRecommendationsByIdPlaysNewQueue() {
        playbackInitiator.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo).subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        verify(playSessionController).playNewQueue(eq(createPlayQueueFromUrns(playSessionSource, TRACK1)), eq(TRACK1), eq(0), anyBoolean(), eq(playSessionSource));
    }

    @Test
    public void startPlaybackWithRecommendationsPlaysNewQueueWithRelatedTracks() {
        playbackInitiator.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo).subscribe(observer);

        verify(playSessionController).playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), eq(true), any(PlaySessionSource.class));
    }

    @Test
    public void playStationResumesIfStationAlreadyStarted() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Station station = StationFixtures.getStation(stationUrn);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(ORIGIN_SCREEN, stationUrn);

        when(playQueueManager.isCurrentTrack(station.getTracks().get(0))).thenReturn(true);
        when(playQueueManager.isCurrentCollectionOrRecommendation(stationUrn)).thenReturn(true);
        when(playSessionController.playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));
        playbackInitiator.playStation(stationUrn, station.getTracks(), playSessionSource, 0).subscribe(observer);

        verifyZeroInteractions(playSessionController);
    }

    @Test
    public void playStationShouldStartFromZeroOnFirstPlay() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Station station = StationFixtures.getStation(stationUrn);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(ORIGIN_SCREEN, stationUrn);
        when(playSessionController.playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));

        playbackInitiator.playStation(stationUrn, station.getTracks(), playSessionSource, Consts.NOT_SET).subscribe(observer);

        final PlayQueue expectedQueue = PlayQueue.fromStation(stationUrn, station.getTracks());
        verify(playSessionController).playNewQueue(expectedQueue, station.getTracks().get(0), 0, false, playSessionSource);
    }

    @Test
    public void playStationShouldStartFromTheNextTrack() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Station station = StationFixtures.getStation(stationUrn, 2);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(ORIGIN_SCREEN, stationUrn);
        when(playSessionController.playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));

        playbackInitiator.playStation(stationUrn, station.getTracks(), playSessionSource, 0).subscribe(observer);

        final PlayQueue expectedQueue = PlayQueue.fromStation(stationUrn, station.getTracks());
        verify(playSessionController).playNewQueue(expectedQueue, station.getTracks().get(1), 1, false, playSessionSource);
    }

    @Test
    public void playStationShouldRestartFromZeroWhenReachingTheEnd() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Station station = StationFixtures.getStation(stationUrn);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(ORIGIN_SCREEN, stationUrn);
        when(playSessionController.playNewQueue(any(PlayQueue.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));

        playbackInitiator.playStation(stationUrn, station.getTracks(), playSessionSource, station.getTracks().size() - 1).subscribe(observer);

        final PlayQueue expectedQueue = PlayQueue.fromStation(stationUrn, station.getTracks());
        verify(playSessionController).playNewQueue(expectedQueue, station.getTracks().get(0), 0, false, playSessionSource);
    }


    private void expectSuccessPlaybackResult() {
        assertThat(observer.getOnNextEvents()).hasSize(1);
        PlaybackResult playbackResult = observer.getOnNextEvents().get(0);
        assertThat(playbackResult.isSuccess()).isTrue();
    }

    private PlayQueue createPlayQueueFromUrns(PlaySessionSource playSessionSource, Urn... trackUrns) {
        return PlayQueue.fromTrackUrnList(Arrays.asList(trackUrns), playSessionSource);
    }

    private PlayQueue createPlayQueue(PlaySessionSource playSessionSource, PropertySet... tracks) {
        return PlayQueue.fromTrackList(Arrays.asList(tracks), playSessionSource);
    }

    private List<Urn> toList(PlayQueue actualQueue) {
        final ArrayList<Urn> result = new ArrayList<>();
        for (PlayQueueItem playQueueItem : actualQueue) {
            result.add(playQueueItem.getTrackUrn());
        }
        return result;
    }
}
