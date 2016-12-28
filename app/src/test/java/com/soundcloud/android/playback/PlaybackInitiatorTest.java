package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayNewQueue;
import static com.soundcloud.android.testsupport.TestUrns.createTrackUrns;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaybackInitiatorTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);
    private static final String EXPLORE_VERSION = "explore-version";
    private static final Screen ORIGIN_SCREEN = Screen.EXPLORE_TRENDING_MUSIC;

    private PlaybackInitiator playbackInitiator;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlaySessionController playSessionController;
    @Mock private PolicyOperations policyOperations;

    @Captor private ArgumentCaptor<PlayQueue> playQueueTracksCaptor;
    @Captor private ArgumentCaptor<Urn> urnArgumentCaptor;

    private TestObserver<PlaybackResult> observer;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setUp() throws Exception {

        playbackInitiator = new PlaybackInitiator(
                playQueueManager,
                playSessionController,
                policyOperations);

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK1));
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());
        when(policyOperations.blockedStatuses(anyListOf(Urn.class))).thenReturn(Observable.just(Collections.<Urn, Boolean>emptyMap()));

        observer = new TestObserver<>();
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"),
                                                          0,
                                                          new Urn("soundcloud:tracks:1"),
                                                          "query");
    }

    @Test
    public void playTrackPlaysNewQueueFromInitialTrack() {
        playbackInitiator.playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN))
                         .subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        assertPlayNewQueue(playSessionController, TestPlayQueue.fromUrns(playSessionSource, TRACK1),
                           TRACK1, 0, playSessionSource);
    }

    @Test
    public void playTrackFixWrongStartingPosition() {
        // This issue has been here forever and we don't know the root cause.
        // This is needed to fix a crash in the context the of the new PQ explicit items feature too.
        // Don't judge me.
        // https://github.com/soundcloud/android/issues/6706
        final int wrongStartPosition = 3;
        playbackInitiator.playTracks(Observable.just(TRACK1).toList(), TRACK1, wrongStartPosition, new PlaySessionSource(ORIGIN_SCREEN))
                         .subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());

        assertPlayNewQueue(playSessionController, TestPlayQueue.fromUrns(playSessionSource, TRACK1), TRACK1, wrongStartPosition, playSessionSource);
    }

    @Test
    public void playTrackPlaysNewQueueFromInitialTrackWithBlockedStatus() {
        when(policyOperations.blockedStatuses(singletonList(TRACK1))).thenReturn(Observable.just(Collections.singletonMap(
                TRACK1,
                true)));

        playbackInitiator.playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN))
                         .subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        final PlayQueue playQueueFromUrns = TestPlayQueue.fromUrns(playSessionSource,
                                                                   Collections.singletonMap(TRACK1, true),
                                                                   TRACK1);
        assertPlayNewQueue(playSessionController, playQueueFromUrns, TRACK1, 0, playSessionSource);
    }

    @Test
    public void playTrackShouldPlayNewQueueIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());

        PlaySessionSource newPlaySessionSource = new PlaySessionSource(Screen.EXPLORE_TRENDING_AUDIO);
        playbackInitiator.playTracks(
                Observable.just(TRACK1).toList(), TRACK1, 0, newPlaySessionSource)
                         .subscribe(observer);

        assertPlayNewQueue(playSessionController,
                           TestPlayQueue.fromUrns(newPlaySessionSource, TRACK1), TRACK1, 0, newPlaySessionSource);
    }

    @Test
    public void playPostsPlaysNewQueueFromInitialTrack() {
        final PlayableWithReposter track = PlayableWithReposter.from(TRACK1);
        playbackInitiator.playPosts(Observable.just(track).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN))
                         .subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        assertPlayNewQueue(playSessionController,
                           TestPlayQueue.fromTracks(playSessionSource, track), TRACK1, 0, playSessionSource);
    }

    @Test
    public void playPostsPlaysNewQueueFromInitialTrackWithBlockedStatus() {
        when(policyOperations.blockedStatuses(singletonList(TRACK1))).thenReturn(Observable.just(Collections.singletonMap(
                TRACK1,
                true)));
        final PlayableWithReposter track = PlayableWithReposter.from(TRACK1);
        playbackInitiator.playPosts(Observable.just(track).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN))
                         .subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        assertPlayNewQueue(playSessionController,
                           TestPlayQueue.fromTracks(playSessionSource, Collections.singletonMap(TRACK1, true), track),
                           TRACK1,
                           0,
                           playSessionSource);
    }

    @Test
    public void playPostsShouldNotSendServiceIntentIfTrackAlreadyPlayingWithSameOriginAndCollectionUrn() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        final PlayableWithReposter track = PlayableWithReposter.from(TRACK1);
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN);
        when(playQueueManager.isCurrentCollection(playSessionSource.getCollectionUrn())).thenReturn(true);
        playbackInitiator.playPosts(Observable.just(track).toList(), TRACK1, 0, playSessionSource).subscribe(observer);

        verify(playSessionController, never()).playNewQueue(any(PlayQueue.class),
                                                            any(Urn.class),
                                                            anyInt(),
                                                            any(PlaySessionSource.class));
    }

    @Test
    public void playTrackCallsPlayCurrentIfTrackAlreadyPlayingWithSameOriginAndSameCollectionUrn() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        final PlayableWithReposter track = PlayableWithReposter.from(TRACK1);
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN);
        when(playQueueManager.isCurrentCollection(playSessionSource.getCollectionUrn())).thenReturn(true);
        playbackInitiator.playPosts(Observable.just(track).toList(), TRACK1, 0, playSessionSource).subscribe(observer);

        verify(playSessionController).playCurrent();
    }

    @Test
    public void playPostsShouldPlayNewQueueIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());

        PlaySessionSource newPlaySessionSource = new PlaySessionSource(Screen.EXPLORE_TRENDING_AUDIO);
        final PlayableWithReposter track = PlayableWithReposter.from(TRACK1);
        playbackInitiator.playPosts(Observable.just(track).toList(), TRACK1, 0, newPlaySessionSource)
                         .subscribe(observer);

        assertPlayNewQueue(playSessionController,
                           TestPlayQueue.fromTracks(newPlaySessionSource, track), TRACK1, 0, newPlaySessionSource);
    }

    @Test
    public void playExploreTrackPlaysNewQueue() {
        final PlaySessionSource playSessionSource = PlaySessionSource.forExplore(ORIGIN_SCREEN.get(), EXPLORE_VERSION);

        playbackInitiator.playTrackWithRecommendationsLegacy(TRACK1, playSessionSource).subscribe(observer);

        final PlaySessionSource expected = PlaySessionSource.forExplore(ORIGIN_SCREEN.get(), EXPLORE_VERSION);

        assertPlayNewQueue(playSessionController, TestPlayQueue.fromUrns(expected, TRACK1), TRACK1, 0, expected);
    }

    @Test
    public void playExploreTrackPlaysNewQueueWithRelatedTracks() {
        playbackInitiator.playTrackWithRecommendationsLegacy(TRACK1, new PlaySessionSource(ORIGIN_SCREEN.get()))
                         .subscribe(observer);

        verify(playSessionController).playNewQueue(any(PlayQueue.class),
                                                   any(Urn.class),
                                                   anyInt(),
                                                   any(PlaySessionSource.class));
    }

    @Test
    public void playFromPlaylistPlaysNewQueue() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);

        final List<Urn> trackUrns = createTrackUrns(tracks.get(0).getId(),
                                                    tracks.get(1).getId(),
                                                    tracks.get(2).getId());

        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(
                ORIGIN_SCREEN.get(), Urn.forPlaylist(456), Urn.forUser(1), trackUrns.size());

        playbackInitiator
                .playTracks(Observable.just(trackUrns), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe(observer);

        final PlayQueue expectedQueue = TestPlayQueue.fromUrns(playSessionSource,
                                                               tracks.get(0).getUrn(),
                                                               tracks.get(1).getUrn(),
                                                               tracks.get(2).getUrn());
        assertPlayNewQueue(playSessionController, expectedQueue, tracks.get(1).getUrn(), 1, playSessionSource);
    }

    @Test
    public void playsNewQueueIfPlayingQueueHasSameContextWithDifferentPlaylistSources() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);

        final List<Urn> trackUrns = createTrackUrns(tracks.get(0).getId(),
                                                    tracks.get(1).getId(),
                                                    tracks.get(2).getId());

        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get()); // same screen origin
        when(playQueueManager.isCurrentCollection(Urn.forPlaylist(1234))).thenReturn(false); // different Playlist Id
        when(playSessionController.playNewQueue(any(PlayQueue.class),
                                                any(Urn.class),
                                                anyInt(),
                                                any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult
                                                                                                                  .success()));

        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(
                Screen.EXPLORE_TRENDING_MUSIC.get(), Urn.forPlaylist(1234), Urn.forUser(1), trackUrns.size());

        playbackInitiator
                .playTracks(Observable.just(trackUrns), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        final PlayQueue expectedQueue = TestPlayQueue.fromUrns(trackUrns, playSessionSource);
        assertPlayNewQueue(playSessionController, expectedQueue,
                           tracks.get(1).getUrn(), 1, playSessionSource);
    }

    @Test
    public void playTracksNoOpsWhenItIsPQMCurrentTrackAndCurrentScreenSourceAndSameCollectionUrn() {
        final String screen = "origin_screen";
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        when(playQueueManager.getScreenTag()).thenReturn(screen);
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.isCurrentCollection(Urn.NOT_SET)).thenReturn(true);

        playbackInitiator
                .playTracks(Observable.just(TRACK1).toList(), TRACK1, 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        verify(playSessionController, never()).playNewQueue(any(PlayQueue.class),
                                                            any(Urn.class),
                                                            anyInt(),
                                                            any(PlaySessionSource.class));
    }

    @Test
    public void playTracksNoOpsWhenItIsPQMCurrentTrackAndCurrentScreenSourceAndCurrentPlaylist() {
        final Urn playlistUrn = Urn.forPlaylist(456L);
        final Urn playlistOwnerUrn = Urn.forUser(789L);
        final String screen = "origin_screen";
        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(
                screen, playlistUrn, playlistOwnerUrn, 2);
        when(playQueueManager.getScreenTag()).thenReturn(screen);
        when(playQueueManager.isCurrentCollection(playlistUrn)).thenReturn(true);
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.isCurrentCollection(playlistUrn)).thenReturn(true);

        playbackInitiator
                .playTracks(Observable.just(TRACK1).toList(), TRACK1, 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        verify(playSessionController, never()).playNewQueue(any(PlayQueue.class),
                                                            any(Urn.class),
                                                            anyInt(),
                                                            any(PlaySessionSource.class));
    }

    @Test
    public void playFromShuffledWithTracksObservablePlaysNewQueueWithGivenTrackIdList() {
        final List<Urn> tracksToPlay = Arrays.asList(TRACK1, TRACK2, TRACK3);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.YOUR_LIKES);

        playbackInitiator.playTracksShuffled(Observable.just(tracksToPlay), playSessionSource)
                         .subscribe(observer);

        verify(playSessionController).playNewQueue(playQueueTracksCaptor.capture(),
                                                   any(Urn.class),
                                                   eq(0),
                                                   eq(playSessionSource));
        final PlayQueue actualQueue = playQueueTracksCaptor.getValue();
        assertThat(actualQueue.getTrackItemUrns()).contains(TRACK1, TRACK2, TRACK3);
    }

    @Test
    public void playTracksShuffledDoesNotLoadRecommendations() {
        final List<Urn> idsOrig = createTrackUrns(1L, 2L, 3L);
        playbackInitiator.playTracksShuffled(Observable.just(idsOrig), new PlaySessionSource(Screen.YOUR_LIKES))
                         .subscribe(observer);

        verify(playSessionController).playNewQueue(any(PlayQueue.class),
                                                   any(Urn.class),
                                                   eq(0),
                                                   any(PlaySessionSource.class));
    }

    @Test
    public void playTracksShuffledReturnPlaybackResultWithValidTrack() {
        final List<Urn> idsOrig = createTrackUrns(1L, 2L, 3L);
        playbackInitiator.playTracksShuffled(Observable.just(idsOrig), new PlaySessionSource(Screen.YOUR_LIKES))
                         .subscribe(observer);

        verify(playSessionController).playNewQueue(any(PlayQueue.class),
                                                   urnArgumentCaptor.capture(),
                                                   eq(0),
                                                   any(PlaySessionSource.class));
        assertThat(urnArgumentCaptor.getValue()).isNotEqualTo(Urn.NOT_SET);
    }

    @Test
    public void playTracksWithNonEmptyTrackListPlaysNewQueue() {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN);
        final List<Urn> tracksToPlay = Arrays.asList(TRACK1, TRACK2, TRACK3, Urn.forPlaylist(1));
        playbackInitiator
                .playTracks(Observable.just(tracksToPlay), TRACK3, 2, playSessionSource)
                .subscribe(observer);

        final PlayQueue expectedPlayQueue = TestPlayQueue.fromUrns(playSessionSource,
                                                                   TRACK1,
                                                                   TRACK2,
                                                                   TRACK3,
                                                                   Urn.forPlaylist(1));
        assertPlayNewQueue(playSessionController, expectedPlayQueue,
                           TRACK3, 2, playSessionSource);
    }

    @Test
    public void playTracksWithEmptyTrackListDoesNotPlayNewQueue() {
        playbackInitiator
                .playTracks(Observable.<Urn>empty().toList(), TRACK1, 2, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe(observer);

        verify(playSessionController, never()).playNewQueue(any(PlayQueue.class),
                                                            any(Urn.class),
                                                            anyInt(),
                                                            any(PlaySessionSource.class));
    }

    @Test
    public void playFromAdapterPlaysNewQueueFromListOfTracks() throws Exception {
        List<Urn> playables = createTrackUrns(1L, 2L);

        playbackInitiator.playTracks(playables, 1, new PlaySessionSource(ORIGIN_SCREEN)).subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        assertPlayNewQueue(playSessionController,
                           TestPlayQueue.fromUrns(playSessionSource, Urn.forTrack(1L), Urn.forTrack(2L)),
                           Urn.forTrack(2L),
                           1,
                           playSessionSource);
    }

    @Test
    public void startPlaybackWithRecommendationsPlaysNewQueue() {
        playbackInitiator.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo)
                         .subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());

        assertPlayNewQueue(playSessionController,
                           TestPlayQueue.fromUrns(playSessionSource, TRACK1), TRACK1, 0, playSessionSource);
    }

    @Test
    public void startPlaybackWithRecommendationsByIdPlaysNewQueue() {
        playbackInitiator.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo)
                         .subscribe(observer);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        assertPlayNewQueue(playSessionController,
                           TestPlayQueue.fromUrns(playSessionSource, TRACK1), TRACK1, 0, playSessionSource);
    }

    @Test
    public void startPlaybackWithRecommendationsPlaysNewQueueWithRelatedTracks() {
        playbackInitiator.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo)
                         .subscribe(observer);

        verify(playSessionController).playNewQueue(any(PlayQueue.class),
                                                   any(Urn.class),
                                                   anyInt(),
                                                   any(PlaySessionSource.class));
    }

    @Test
    public void playStationResumesIfStationAlreadyStarted() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final StationRecord station = StationFixtures.getStation(stationUrn);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(ORIGIN_SCREEN, stationUrn);
        final Urn currentTrackUrn = station.getTracks().get(0).getTrackUrn();

        when(playQueueManager.isCurrentTrack(currentTrackUrn)).thenReturn(true);
        when(playQueueManager.isCurrentCollectionOrRecommendation(stationUrn)).thenReturn(true);
        when(playSessionController.playNewQueue(any(PlayQueue.class),
                                                any(Urn.class),
                                                anyInt(),
                                                any(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));
        playbackInitiator.playStation(stationUrn, station.getTracks(), playSessionSource, currentTrackUrn, 0)
                         .subscribe(observer);

        verifyZeroInteractions(playSessionController);
    }

    @Test
    public void playStationPlaysNewQueue() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final StationRecord station = StationFixtures.getStation(stationUrn);
        final PlaySessionSource playSource = PlaySessionSource.forStation(ORIGIN_SCREEN, stationUrn);

        when(playQueueManager.isCurrentTrack(any(Urn.class))).thenReturn(false);
        when(playSessionController.playNewQueue(any(PlayQueue.class),
                                                any(Urn.class),
                                                anyInt(),
                                                any(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        playbackInitiator.playStation(stationUrn, station.getTracks(), playSource, Urn.NOT_SET, 0)
                         .subscribe(observer);

        verify(playSessionController).playNewQueue(any(PlayQueue.class),
                                                   any(Urn.class),
                                                   anyInt(),
                                                   any(PlaySessionSource.class));
    }

    @Test
    public void includeExplicitContentFromPreviousQueue() {
        final PlayQueueItem explicitTrack1 = TestPlayQueueItem.createTrack(Urn.forTrack(123l));
        final PlayQueueItem explicitTrack2 = TestPlayQueueItem.createTrack(Urn.forTrack(1234l));
        final List<PlayQueueItem> currentPlayQueue = Arrays.asList(explicitTrack1, explicitTrack2);
        when(playQueueManager.getUpcomingExplicitQueueItems()).thenReturn(currentPlayQueue);

        final List<Urn> newTrackList = Arrays.asList(Urn.forTrack(1), Urn.forTrack(2), Urn.forTrack(3));
        playbackInitiator.playTracks(newTrackList, 0, PlaySessionSource.EMPTY).subscribe();

        verify(playSessionController).playNewQueue(playQueueTracksCaptor.capture(),
                                                   eq(Urn.forTrack(1)),
                                                   eq(0),
                                                   eq(PlaySessionSource.EMPTY));


        PlayQueue createdPlayQueue = playQueueTracksCaptor.getValue();
        assertThat(createdPlayQueue.size()).isEqualTo(5);
        assertThat(createdPlayQueue.getUrn(0).getNumericId()).isEqualTo(1);
        assertThat(createdPlayQueue.getUrn(1).getNumericId()).isEqualTo(123);
        assertThat(createdPlayQueue.getUrn(2).getNumericId()).isEqualTo(1234);
        assertThat(createdPlayQueue.getUrn(3).getNumericId()).isEqualTo(2);
        assertThat(createdPlayQueue.getUrn(4).getNumericId()).isEqualTo(3);
    }

    @Test
    public void addExplicitContentToEndWhenStartPositionisHigherThanNumberOfTracks() {
        final PlayQueueItem explicitTrack1 = TestPlayQueueItem.createTrack(Urn.forTrack(123l));
        final PlayQueueItem explicitTrack2 = TestPlayQueueItem.createTrack(Urn.forTrack(1234l));
        final List<PlayQueueItem> currentPlayQueue = Arrays.asList(explicitTrack1, explicitTrack2);
        when(playQueueManager.getUpcomingExplicitQueueItems()).thenReturn(currentPlayQueue);

        final List<Urn> newTrackList = Arrays.asList(Urn.forTrack(1), Urn.forTrack(2), Urn.forTrack(3));
        playbackInitiator.playTracks(newTrackList, 2, PlaySessionSource.EMPTY).subscribe();

        verify(playSessionController).playNewQueue(playQueueTracksCaptor.capture(),
                                                   eq(Urn.forTrack(3)),
                                                   eq(2),
                                                   eq(PlaySessionSource.EMPTY));


        PlayQueue createdPlayQueue = playQueueTracksCaptor.getValue();
        assertThat(createdPlayQueue.getUrn(0).getNumericId()).isEqualTo(1);
        assertThat(createdPlayQueue.getUrn(1).getNumericId()).isEqualTo(2);
        assertThat(createdPlayQueue.getUrn(2).getNumericId()).isEqualTo(3);
        assertThat(createdPlayQueue.getUrn(3).getNumericId()).isEqualTo(123);
        assertThat(createdPlayQueue.getUrn(4).getNumericId()).isEqualTo(1234);
    }

    private void expectSuccessPlaybackResult() {
        assertThat(observer.getOnNextEvents()).hasSize(1);
        PlaybackResult playbackResult = observer.getOnNextEvents().get(0);
        assertThat(playbackResult.isSuccess()).isTrue();
    }

}
