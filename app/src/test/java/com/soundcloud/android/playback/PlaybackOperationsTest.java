package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.TestUrns.createTrackUrns;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlayQueueOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class PlaybackOperationsTest extends PlatformUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);
    private static final String EXPLORE_VERSION = "explore-version";
    private static final Screen ORIGIN_SCREEN = Screen.EXPLORE_TRENDING_MUSIC;

    private PlaybackOperations playbackOperations;

    private PublicApiPlaylist playlist;

    @Mock private ServiceInitiator serviceInitiator;
    @Mock private ScModelManager modelManager;
    @Mock private TrackStorage trackStorage;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private AdsOperations adsOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaybackStrategy playbackStrategy;
    @Mock private PlayQueueOperations playQueueOperations;

    @Captor private ArgumentCaptor<List<Urn>> playQueueTracksCaptor;

    private TestObserver<PlaybackResult> observer;
    private TestEventBus eventBus = new TestEventBus();
    private SearchQuerySourceInfo searchQuerySourceInfo;


    @Before
    public void setUp() throws Exception {

        final Provider<PlaybackStrategy> playbackStrategyProvider = new Provider<PlaybackStrategy>() {
            @Override
            public PlaybackStrategy get() {
                return playbackStrategy;
            }
        };

        playbackOperations = new PlaybackOperations(
                serviceInitiator,
                modelManager,
                trackStorage,
                playQueueManager,
                playSessionStateProvider,
                playbackToastHelper,
                eventBus,
                adsOperations,
                accountOperations,
                playQueueOperations,
                playbackStrategyProvider);

        playlist = ModelFixtures.create(PublicApiPlaylist.class);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK1);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        observer = new TestObserver<>();
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, new Urn("soundcloud:tracks:1"));
    }

    @Test
     public void playTrackPlaysNewQueueFromInitialTrack() {
        playbackOperations.playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN)).subscribe(observer);

        verify(playbackStrategy).playNewQueue(Arrays.asList(TRACK1), TRACK1, 0, false, new PlaySessionSource(ORIGIN_SCREEN.get()));
    }

    @Test
    public void playTrackShouldNotSendServiceIntentIfTrackAlreadyPlayingWithSameOrigin() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        playbackOperations.playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN));

        verifyZeroInteractions(serviceInitiator);
    }

    @Test
    public void playTrackShouldPlayNewQueueIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());

        PlaySessionSource newPlaySessionSource = new PlaySessionSource(Screen.EXPLORE_TRENDING_AUDIO);
        playbackOperations.playTracks(
                Observable.just(TRACK1).toList(), TRACK1, 0, newPlaySessionSource)
                .subscribe(observer);

        verify(playbackStrategy).playNewQueue(Arrays.asList(TRACK1), TRACK1, 0, false, newPlaySessionSource);
    }

    @Test
    public void playExploreTrackPlaysNewQueue() {
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setExploreVersion(EXPLORE_VERSION);

        playbackOperations.playTrackWithRecommendationsLegacy(TRACK1, playSessionSource).subscribe(observer);

        final PlaySessionSource expected = new PlaySessionSource(ORIGIN_SCREEN.get());
        expected.setExploreVersion(EXPLORE_VERSION);

        verify(playbackStrategy).playNewQueue(eq(Arrays.asList(TRACK1)), eq(TRACK1), eq(0), anyBoolean(), eq(expected));
    }

    @Test
    public void playExploreTrackPlaysNewQueueWithRelatedTracks() {
        playbackOperations.playTrackWithRecommendationsLegacy(TRACK1, new PlaySessionSource(ORIGIN_SCREEN.get())).subscribe(observer);

        verify(playbackStrategy).playNewQueue(anyListOf(Urn.class), any(Urn.class), anyInt(), eq(true), any(PlaySessionSource.class));
    }

    @Test
    public void playTrackWithRecommendationsReturnsAnErrorWhenNoRecommendation() {
        when(playQueueOperations.getRelatedTracksUrns(TRACK1)).thenReturn(Observable.<Urn>error(new NoSuchElementException()));

        playbackOperations.playTrackWithRecommendations(TRACK1, new PlaySessionSource(ORIGIN_SCREEN), 0).subscribe(observer);

        assertThat(observer.getOnErrorEvents()).hasSize(1);
    }

    @Test
    public void playTrackWithRecommendationsPlaysQueueWithSeedAtSpecifiedPosition() {
        final List<Urn> relatedUrns = new LinkedList<>(Arrays.asList(TRACK2, TRACK3));
        when(playQueueOperations.getRelatedTracksUrns(TRACK1)).thenReturn(Observable.from(relatedUrns));

        playbackOperations.playTrackWithRecommendations(TRACK1, new PlaySessionSource(ORIGIN_SCREEN), 0).subscribe(observer);

        final List<Urn> expectedUrns = Arrays.asList(TRACK1, TRACK2, TRACK3);
        verify(playbackStrategy).playNewQueue(eq(expectedUrns), any(Urn.class), anyInt(), eq(false), any(PlaySessionSource.class));
    }

    @Test
    public void playFromPlaylistPlaysNewQueue() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);

        final List<Urn> trackUrns = createTrackUrns(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        playbackOperations
                .playTracks(Observable.just(trackUrns), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe(observer);

        verify(playbackStrategy).playNewQueue(Arrays.asList(tracks.get(0).getUrn(), tracks.get(1).getUrn(), tracks.get(2).getUrn()), tracks.get(1).getUrn(), 1, false, playSessionSource);
    }

    @Test
    public void playsNewQueueIfPlayingQueueHasSameContextWithDifferentPlaylistSources() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);

        final List<Urn> trackUrns = createTrackUrns(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get()); // same screen origin
        when(playQueueManager.isPlaylist()).thenReturn(true);
        when(playQueueManager.getPlaylistUrn()).thenReturn(Urn.forPlaylist(1234)); // different Playlist Id
        when(playbackStrategy.playNewQueue(anyListOf(Urn.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.EXPLORE_TRENDING_MUSIC.get());
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        playbackOperations
                .playTracks(Observable.just(tracks.get(1).getUrn()).toList(), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        verify(playbackStrategy).playNewQueue(Arrays.asList(tracks.get(1).getUrn()), tracks.get(1).getUrn(), 1, false, playSessionSource);
    }

    @Test
    public void playTracksNoOpsWhenItIsPQMCurrentTrackAndCurrentScreenSource()  {
        final String screen = "origin_screen";
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        when(playQueueManager.getScreenTag()).thenReturn(screen);
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.isPlaylist()).thenReturn(false);

        playbackOperations
                .playTracks(Observable.just(TRACK1).toList(), TRACK1, 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        verify(playbackStrategy, never()).playNewQueue(anyListOf(Urn.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class));
    }

    @Test
    public void playTracksNoOpsWhenItIsPQMCurrentTrackAndCurrentScreenSourceAndCurrentPlaylist()  {
        final Urn playlistUrn = Urn.forPlaylist(456L);
        final Urn playlistOwnerUrn = Urn.forUser(789L);
        final String screen = "origin_screen";
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setPlaylist(playlistUrn, playlistOwnerUrn);
        when(playQueueManager.getScreenTag()).thenReturn(screen);
        when(playQueueManager.getPlaylistUrn()).thenReturn(playlistUrn);
        when(playQueueManager.isPlaylist()).thenReturn(true);
        when(playQueueManager.isCurrentTrack(TRACK1)).thenReturn(true);
        when(playQueueManager.isCurrentPlaylist(playlistUrn)).thenReturn(true);

        playbackOperations
                .playTracks(Observable.just(TRACK1).toList(), TRACK1, 1, playSessionSource)
                .subscribe(observer);

        expectSuccessPlaybackResult();
        verify(playbackStrategy, never()).playNewQueue(anyListOf(Urn.class), any(Urn.class), anyInt(), anyBoolean(), any(PlaySessionSource.class));
    }

    @Test
    public void togglePlaybackShouldTogglePlaybackOnPlaybackStrategyIfPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        playbackOperations.togglePlayback();

        verify(playbackStrategy).togglePlayback();
    }

    @Test
    public void togglePlaybackShouldNotTogglePlaybackOnPlaybackStrategyIfNotPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(false);
        playbackOperations.togglePlayback();

        verify(playbackStrategy, never()).togglePlayback();
    }

    @Test
    public void playCurrentCallsPlayCurrentOnPlaybackStrategy() {
        playbackOperations.playCurrent();

        verify(playbackStrategy).playCurrent();
    }

    @Test
    public void shouldUpdatePlayPositionToGivenIndex() {
        playbackOperations.setPlayQueuePosition(5);

        verify(playQueueManager).setPosition(5);
    }

    @Test
    public void settingPlayQueuePositionPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);

        playbackOperations.setPlayQueuePosition(5);

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentTrackAudioAd();
        inOrder.verify(playQueueManager).setPosition(5);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(event.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void settingPlayQueuePositionDoesNotPublishAdSkippedTrackingEventWhenTrackNotAnAd() {
        playbackOperations.setPlayQueuePosition(5);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
     public void previousTrackCallsMoveToPreviousTrackOnPlayQueueManagerIfProgressLessThanTolerance() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(2999L, 5000));

        playbackOperations.previousTrack();

        verify(playQueueManager).moveToPreviousTrack();
    }

    @Test
    public void previousTrackCallsMoveToPreviousTrackOnPlayQueueManagerIfProgressEqualToleranceAndPlayingAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        playbackOperations.previousTrack();

        verify(playQueueManager).moveToPreviousTrack();
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressEqualToTolerance() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        playbackOperations.previousTrack();

        verify(playbackStrategy).seek(0);
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressGreaterThanTolerance() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3001L, 5000));

        playbackOperations.previousTrack();

        verify(playbackStrategy).seek(0);
    }

    @Test
    public void previousTrackCallsPreviousTrackIfPlayingAudioAdWithProgressEqualToTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(PlaybackProgress.empty());

        playbackOperations.previousTrack();

        verify(playQueueManager).moveToPreviousTrack();
    }

    @Test
    public void previousTrackDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.previousTrack();

        verify(playQueueManager, never()).moveToPreviousTrack();
    }

    @Test
    public void previousTrackShowsUnskippableToastWhenPlaybackNotSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.previousTrack();

        verify(playbackToastHelper).showUnskippableAdToast();
    }

    @Test
    public void previousTrackPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        playbackOperations.previousTrack();

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentTrackAudioAd();
        inOrder.verify(playQueueManager).moveToPreviousTrack();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(event.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void previousTrackDoesNotPublishAdSkippedTrackingEventWhenAdNotYetSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.previousTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void previousTrackDoesNotPublishAdSkippedTrackingEventWhenTrackNotAnAd() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        playbackOperations.previousTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void nextTrackShowsUnskippableToastWhenPlaybackNotSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.nextTrack();

        verify(playbackToastHelper).showUnskippableAdToast();
    }

    @Test
    public void nextTrackCallsNextTrackOnPlayQueueManager() {
        playbackOperations.nextTrack();

        verify(playQueueManager).nextTrack();
    }

    @Test
    public void nextTrackCallsNextTrackIfPlayingAudioAdWithProgressEqualToTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        playbackOperations.nextTrack();

        verify(playQueueManager).nextTrack();
    }

    @Test
    public void nextTrackDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.nextTrack();

        verify(playQueueManager, never()).nextTrack();
    }

    @Test
    public void nextTrackPublishesAdSkippedTrackingEventWhenTrackIsAudioAd() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);

        playbackOperations.nextTrack();

        // make sure we test for the current track being an ad *before* we skip
        InOrder inOrder = inOrder(adsOperations, playQueueManager);
        inOrder.verify(adsOperations, atLeastOnce()).isCurrentTrackAudioAd();
        inOrder.verify(playQueueManager).nextTrack();

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(event.getAttributes().get("ad_track_urn")).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void nextTrackDoesNotPublishAdSkippedTrackingEventWhenAdNotYetSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.nextTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void nextTrackDoesNotPublishAdSkippedTrackingEventWhenTrackNotAnAd() {
        playbackOperations.nextTrack();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void seeksToProvidedPositionIfServiceIsPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        playbackOperations.seek(350L);

        verify(playbackStrategy).seek(350L);
    }

    @Test
    public void seeksSavesPlayQueueProgressToSeekPositionIfNotPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(false);
        playbackOperations.seek(350L);

        verify(playQueueManager).saveCurrentProgress(350L);
    }

    @Test
    public void seekSeeksToProvidedPositionIfPlayingAudioAdWithProgressEqualTimeout() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);

        playbackOperations.seek(350L);

        verify(playbackStrategy).seek(350L);
    }

    @Test
    public void seekDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.seek(350L);

        verifyZeroInteractions(serviceInitiator);
    }

    @Test
    public void stopServiceSendsStopActionToService() {
        playbackOperations.stopService();

        verify(serviceInitiator).stopPlaybackService();
    }

    @Test
    public void playFromShuffledWithTracksObservablePlaysNewQueueWithGivenTrackIdList() {
        final List<Urn> tracksToPlay = Arrays.asList(TRACK1, TRACK2, TRACK3);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.YOUR_LIKES);

        playbackOperations.playTracksShuffled(Observable.just(tracksToPlay), playSessionSource).subscribe(observer);

        verify(playbackStrategy).playNewQueue(playQueueTracksCaptor.capture(), any(Urn.class), eq(0), anyBoolean(), eq(playSessionSource));
        assertThat(playQueueTracksCaptor.getValue()).contains(TRACK1, TRACK2, TRACK3);
    }

    @Test
    public void playTracksShuffledDoesNotLoadRecommendations() {
        final List<Urn> idsOrig = createTrackUrns(1L, 2L, 3L);
        playbackOperations.playTracksShuffled(Observable.just(idsOrig), new PlaySessionSource(Screen.YOUR_LIKES)).subscribe(observer);

        verify(playbackStrategy).playNewQueue(anyListOf(Urn.class), any(Urn.class), eq(0), eq(false), any(PlaySessionSource.class));
    }

    @Test
    public void playTracksWithNonEmptyTrackListPlaysNewQueue() {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN);
        playbackOperations
                .playTracks(Observable.just(TRACK1).toList(), TRACK1, 2, playSessionSource)
                .subscribe(observer);

        verify(playbackStrategy).playNewQueue(Arrays.asList(TRACK1), TRACK1, 2, false, playSessionSource);
    }

    @Test
    public void playTracksWithEmptyTrackListDoesNotPlayNewQueue() {
        playbackOperations
                .playTracks(Observable.<Urn>empty().toList(), TRACK1, 2, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe(observer);

        verify(playbackStrategy, never()).playNewQueue(anyListOf(Urn.class), any(Urn.class), anyInt(), eq(false), any(PlaySessionSource.class));
    }

    @Test
    public void playFromAdapterPlaysNewQueueFromListOfTracks() throws Exception {
        List<Urn> playables = createTrackUrns(1L, 2L);

        playbackOperations.playTracks(playables, 1, new PlaySessionSource(ORIGIN_SCREEN)).subscribe(observer);

        verify(playbackStrategy).playNewQueue(Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L)), Urn.forTrack(2L), 1, false, new PlaySessionSource(ORIGIN_SCREEN.get()));
    }

    @Test
    public void startPlaybackWithRecommendationsPlaysNewQueue() {
        playbackOperations.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo).subscribe(observer);

        verify(playbackStrategy).playNewQueue(
                eq(Arrays.asList(TRACK1)),
                eq(TRACK1),
                eq(0),
                anyBoolean(),
                eq(new PlaySessionSource(ORIGIN_SCREEN.get())));
    }

    @Test
    public void startPlaybackWithRecommendationsByIdPlaysNewQueue() {
        playbackOperations.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo).subscribe(observer);

        verify(playbackStrategy).playNewQueue(eq(Arrays.asList(TRACK1)), eq(TRACK1), eq(0), anyBoolean(), eq(new PlaySessionSource(ORIGIN_SCREEN.get())));
    }

    @Test
    public void startPlaybackWithRecommendationsPlaysNewQueueWithRelatedTracks() {
        playbackOperations.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo).subscribe(observer);

        verify(playbackStrategy).playNewQueue(anyListOf(Urn.class), any(Urn.class), anyInt(), eq(true), any(PlaySessionSource.class));
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayTrack() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe(observer);

        expectUnskippablePlaybackResult();
    }

    @Test
    public void allowSkippingWhenAdIsSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());

        playbackOperations.playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe(observer);

        verify(playbackStrategy).playNewQueue(Arrays.asList(TRACK1), TRACK1, 0, false, playSessionSource);
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayFromAdapter() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        final List<Urn> tracks = createTrackUrns(1L);
        final List<Urn> trackUrns = createTrackUrns(1L);
        when(trackStorage.getTracksForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.just(trackUrns));

        Urn initialTrack = tracks.get(0);
        playbackOperations
                .playTracksFromUri(Content.ME_LIKES.uri, 0, initialTrack, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe(observer);

        expectUnskippablePlaybackResult();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayPlaylistFromPosition() throws CreateModelException {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(createTrackUrns(123L)));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        playbackOperations
                .playTracks(Observable.just(TRACK1).toList(), TRACK1, 0, playSessionSource)
                .subscribe(observer);

        expectUnskippablePlaybackResult();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnRecommendations() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.startPlaybackWithRecommendations(TRACK1, ORIGIN_SCREEN, searchQuerySourceInfo).subscribe(observer);

        expectUnskippablePlaybackResult();
    }

    private void setupAdInProgress(long currentProgress) {
        final PlaybackProgress progress = new PlaybackProgress(currentProgress, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(456L)));
    }

    private void expectSuccessPlaybackResult() {
        assertThat(observer.getOnNextEvents()).hasSize(1);
        PlaybackResult playbackResult = observer.getOnNextEvents().get(0);
        assertThat(playbackResult.isSuccess()).isTrue();
    }

    private void expectUnskippablePlaybackResult() {
        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents().get(0).getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.UNSKIPPABLE);
    }

}
