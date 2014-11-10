package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.createNewUserPlaylist;
import static com.soundcloud.android.testsupport.TestHelper.createTracksUrn;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.playback.ui.view.PlaybackToastViewController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackOperationsTest {

    private static final String EXPLORE_VERSION = "explore-version";
    private static final Screen ORIGIN_SCREEN = Screen.EXPLORE_TRENDING_MUSIC;
    public static final Urn TRACK_URN = Urn.forTrack(123L);

    private PlaybackOperations playbackOperations;

    private PublicApiTrack track;
    private PublicApiPlaylist playlist;

    @Mock private ScModelManager modelManager;
    @Mock private TrackStorage trackStorage;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlaybackToastViewController playbackToastViewController;
    @Mock private AdsOperations adsOperations;
    @Mock private AccountOperations accountOperations;
    private TestObserver<List<Urn>> observer;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        playbackOperations = new PlaybackOperations(Robolectric.application, modelManager, trackStorage,
                playQueueManager, playSessionStateProvider, playbackToastViewController, eventBus, adsOperations, accountOperations);
        track = ModelFixtures.create(PublicApiTrack.class);
        playlist = ModelFixtures.create(PublicApiPlaylist.class);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        observer = new TestObserver<>();
    }

    @Test
     public void playTrackSetsPlayQueueOnPlayQueueManagerFromInitialTrack() {
        Urn track1 = track.getUrn();
        playbackOperations.playTracks(Observable.just(track1), track1, 0, new PlaySessionSource(ORIGIN_SCREEN)).subscribe();

        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void playTrackOpensCurrentTrackThroughService() {
        Urn track1 = track.getUrn();
        playbackOperations.playTracks(Observable.just(track1), track1, 0, new PlaySessionSource(ORIGIN_SCREEN)).subscribe();

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playTrackShouldNotSendServiceIntentIfTrackAlreadyPlayingWithSameOrigin() {
        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);
        Urn track1 = track.getUrn();
        playbackOperations.playTracks(Observable.just(track1), track1, 0, new PlaySessionSource(ORIGIN_SCREEN));

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).toBeNull();
    }

    @Test
    public void playTrackShouldSendServiceIntentIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());

        Urn track1 = track.getUrn();
        playbackOperations.playTracks(Observable.just(track1), track1, 0, new PlaySessionSource(Screen.EXPLORE_TRENDING_AUDIO)).subscribe();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).not.toBeNull();
    }

    @Test
    public void playExploreTrackSetsPlayQueueAndOriginOnPlayQueueManager() {
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setExploreVersion(EXPLORE_VERSION);

        playbackOperations.playTrackWithRecommendations(track.getUrn(), playSessionSource).subscribe();

        final PlaySessionSource expected = new PlaySessionSource(ORIGIN_SCREEN.get());
        expected.setExploreVersion(EXPLORE_VERSION);

        checkSetNewPlayQueueArgs(0, expected, track.getId());
    }

    @Test
    public void playExploreTrackPlaysCurrentTrackThroughService() {
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setExploreVersion(EXPLORE_VERSION);

        playbackOperations.playTrackWithRecommendations(track.getUrn(), playSessionSource).subscribe();

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playExploreTrackCallsFetchRelatedTracksOnPlayQueueManager() {
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setExploreVersion(EXPLORE_VERSION);

        playbackOperations.playTrackWithRecommendations(track.getUrn(), playSessionSource).subscribe();

        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void playFromPlaylistSetsNewPlayqueueOnPlayQueueManagerFromPlaylist() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);
        PublicApiPlaylist playlist = createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final List<Urn> trackUrns = createTracksUrn(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        playbackOperations
                .playTracks(Observable.from(trackUrns), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe();

        checkSetNewPlayQueueArgs(1, playSessionSource, tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playFromPlaylistPlaysCurrentTrackThroughPlaybackService() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);
        PublicApiPlaylist playlist = createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final List<Urn> trackUrns = createTracksUrn(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());
        playbackOperations
                .playTracks(Observable.just(tracks.get(1).getUrn()), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe();

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startsServiceIfPlayingQueueHasSameContextWithDifferentPlaylistSources() throws CreateModelException {
        List<PublicApiTrack> tracks = ModelFixtures.create(PublicApiTrack.class, 3);
        PublicApiPlaylist playlist = createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final List<Urn> trackUrns = createTracksUrn(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get()); // same screen origin
        when(playQueueManager.isPlaylist()).thenReturn(true);
        when(playQueueManager.getPlaylistUrn()).thenReturn(Urn.forPlaylist(1234)); // different Playlist Id

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.EXPLORE_TRENDING_MUSIC.get());
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        playbackOperations
                .playTracks(Observable.just(tracks.get(1).getUrn()), tracks.get(1).getUrn(), 1, playSessionSource)
                .subscribe(observer);
        expect(observer.getOnNextEvents()).not.toBeEmpty();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).not.toBeNull();
    }

    @Test
    public void playTracksNoOpsWhenItIsPQMCurrentTrackAndCurrentScreenSource()  {
        final String screen = "origin_screen";
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        when(playQueueManager.getScreenTag()).thenReturn(screen);
        when(playQueueManager.isCurrentTrack(TRACK_URN)).thenReturn(true);
        when(playQueueManager.isPlaylist()).thenReturn(false);
        playbackOperations
                .playTracks(Observable.just(TRACK_URN), TRACK_URN, 1, playSessionSource)
                .subscribe(observer);

        expect(observer.getOnNextEvents()).toBeEmpty();
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
        when(playQueueManager.isCurrentTrack(TRACK_URN)).thenReturn(true);
        when(playQueueManager.isCurrentPlaylist(playlistUrn)).thenReturn(true);

        playbackOperations
                .playTracks(Observable.just(TRACK_URN), TRACK_URN, 1, playSessionSource)
                .subscribe(observer);
        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void togglePlaybackShouldSendTogglePlaybackIntentIfPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        playbackOperations.togglePlayback();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toBe(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION);
    }

    @Test
    public void togglePlaybackShouldPlayCurrentIfNotPlayingCurrentTrack() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(false);
        playbackOperations.togglePlayback();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toBe(PlaybackService.Actions.PLAY_CURRENT);
    }

    @Test
    public void shouldPlayCurrentQueueTrack() {
        playbackOperations.playCurrent();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toBe(PlaybackService.Actions.PLAY_CURRENT);
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
        expect(event.getKind()).toEqual(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        expect(event.getAttributes().get("ad_track_urn")).toEqual(Urn.forTrack(123).toString());
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

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(0L);
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressGreaterThanTolerance() {
        when(playSessionStateProvider.isPlayingCurrentPlayQueueTrack()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3001L, 5000));

        playbackOperations.previousTrack();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(0L);
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

        verify(playbackToastViewController).showUnkippableAdToast();
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
        expect(event.getKind()).toEqual(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        expect(event.getAttributes().get("ad_track_urn")).toEqual(Urn.forTrack(123).toString());
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

        verify(playbackToastViewController).showUnkippableAdToast();
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
        expect(event.getKind()).toEqual(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        expect(event.getAttributes().get("ad_track_urn")).toEqual(Urn.forTrack(123).toString());
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

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(350L);
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

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(350L);
    }

    @Test
    public void seekDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.seek(350L);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).toBeNull();
    }

    @Test
    public void stopServiceSendsStopActionToService() {
        playbackOperations.stopService();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.STOP_ACTION);
    }

    @Test
    public void playFromIdsShuffledSetsPlayQueueOnPlayQueueManagerWithGivenTrackIdList() {
        final List<Urn> idsOrig = createTracksUrn(1L, 2L, 3L);
        playbackOperations.playTracksShuffled(idsOrig, new PlaySessionSource(Screen.YOUR_LIKES)).subscribe();

        ArgumentCaptor<PlayQueue> playQueueCaptor = ArgumentCaptor.forClass(PlayQueue.class);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.YOUR_LIKES);

        verify(playQueueManager).setNewPlayQueue(playQueueCaptor.capture(), eq(0), eq(playSessionSource));

        final PlayQueue playQueue = playQueueCaptor.getValue();
        expect(playQueue.size()).toEqual(3);
        List<Long> shuffledIds = new ArrayList<Long>(3);
        for (int i = 0; i < playQueue.size(); i++){
            shuffledIds.add(playQueue.getTrackId(i));
        }
        expect(shuffledIds).toContainExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    public void playFromIdsShuffledOpensCurrentTrackThroughPlaybackService() {
        final List<Urn> idsOrig = createTracksUrn(1L, 2L, 3L);

        playbackOperations.playTracksShuffled(idsOrig, new PlaySessionSource(Screen.YOUR_LIKES)).subscribe();

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playTracksWithTrackListContainsTracksOpensCurrentTrackThroughPlaybackService() {
        final Observable<Urn> tracks = Observable.just(Urn.forTrack(123L));
        playbackOperations
                .playTracks(tracks, TRACK_URN, 2, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe();
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playTracksWithEmptyTrackListDoesNotOpenCurrentTrackThroughPlaybackService() {
        Observable<Urn> tracks = Observable.empty();
        playbackOperations
                .playTracks(tracks, TRACK_URN, 2, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe();
        expectLastStartedServiceToBeNull();
    }

    @Test
    public void playFromAdapterShouldRemoveDuplicates() throws Exception {
        List<Urn> playables = createTracksUrn(1L, 2L, 3L, 2L, 1L);

        playbackOperations.playTracks(playables, 4, new PlaySessionSource(ORIGIN_SCREEN)).subscribe();

        checkSetNewPlayQueueArgs(2, new PlaySessionSource(ORIGIN_SCREEN.get()), 2L, 3L, 1L);
    }

    @Test
    public void playFromAdapterSetsPlayQueueOnPlayQueueManagerFromListOfTracks() throws Exception {
        List<Urn> playables = createTracksUrn(1L, 2L);

        playbackOperations.playTracks(playables, 1, new PlaySessionSource(ORIGIN_SCREEN)).subscribe();

        checkSetNewPlayQueueArgs(1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterOpensCurrentTrackThroughPlaybackService() throws Exception {
        List<Urn> playables = createTracksUrn(1L, 2L);

        playbackOperations.playTracks(playables, 1, new PlaySessionSource(ORIGIN_SCREEN)).subscribe();

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playFromAdapterShouldFallBackToPositionZeroIfInitialItemNotFound()  {
        final List<Urn> playables = createTracksUrn(1L, 2L);
        final List<Urn> ids = createTracksUrn(6L, 7L);

        when(trackStorage.getTracksForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.just(ids));
        Urn initialTrack = playables.get(1);
        playbackOperations
                .playTracksFromUri(Content.ME_LIKES.uri, 1, initialTrack, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe();


        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), 6L, 7L);
    }

    @Test
    public void startPlaybackWithRecommendationsCachesTrack() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);

        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN).subscribe();

        verify(modelManager).cache(track);
    }

    @Test
    public void startPlaybackWithRecommendationsSetsConfiguredPlayQueueOnPlayQueueManager() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);

        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN).subscribe();

        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void startPlaybackWithRecommendationsOpensCurrentThroughPlaybackService() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);

        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN).subscribe();

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startPlaybackWithRecommendationsByTrackCallsFetchRecommendationsOnPlayQueueManager() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);

        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN).subscribe();

        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void startPlaybackWithRecommendationsByIdSetsPlayQueueOnPlayQueueManager() {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN).subscribe();

        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), 123L);
    }

    @Test
    public void startPlaybackWithRecommendationsByIdOpensCurrentThroughPlaybackService() {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN).subscribe();

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startPlaybackWithRecommendationsByIdCallsFetchRelatedOnPlayQueueManager() {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN).subscribe();

        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayTrack() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        Urn track1 = track.getUrn();
        playbackOperations.playTracks(Observable.just(track1), track1, 0, new PlaySessionSource(ORIGIN_SCREEN)).subscribe(observer);

        expectUnskippableException();
    }

    @Test
    public void allowSkippingWhenAdIsSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());

        Urn track1 = track.getUrn();
        playbackOperations.playTracks(Observable.just(track1), track1, 0, new PlaySessionSource(ORIGIN_SCREEN)).subscribe();

        checkSetNewPlayQueueArgs(0, playSessionSource, track.getId());
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayFromAdapter() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        final List<Urn> tracks = createTracksUrn(1L);
        final List<Urn> trackUrns = createTracksUrn(1L);
        when(trackStorage.getTracksForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.just(trackUrns));

        Urn initialTrack = tracks.get(0);
        playbackOperations
                .playTracksFromUri(Content.ME_LIKES.uri, 0, initialTrack, new PlaySessionSource(ORIGIN_SCREEN))
                .subscribe(observer);

        expectUnskippableException();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayFromShuffledIds() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        final List<Urn> trackUrns = createTracksUrn(1L);

        playbackOperations.playTracksShuffled(trackUrns, new PlaySessionSource(Screen.YOUR_LIKES)).subscribe(observer);

        expectUnskippableException();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayPlaylistFromPosition() throws CreateModelException {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(createTracksUrn(123L)));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        playbackOperations
                .playTracks(Observable.just(TRACK_URN), TRACK_URN, 0, playSessionSource)
                .subscribe(observer);

        expectUnskippableException();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnRecommendations() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN).subscribe(observer);

        expectUnskippableException();
    }

    private void setupAdInProgress(long currentProgress) {
        final PlaybackProgress progress = new PlaybackProgress(currentProgress, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(456L)));
    }

    private void checkSetNewPlayQueueArgs(int startPosition, PlaySessionSource playSessionSource, Long... ids){
        verify(playQueueManager).setNewPlayQueue(
                eq(PlayQueue.fromTrackUrnList(createTracksUrn(ids), playSessionSource)), eq(startPosition),
                eq(playSessionSource));
    }

    protected void checkLastStartedServiceForPlayCurrentAction() {
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toEqual(PlaybackService.Actions.PLAY_CURRENT);
    }

    private void expectLastStartedServiceToBeNull() {
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).toBeNull();
    }

    private void expectUnskippableException() {
        expect(observer.getOnErrorEvents().get(0)).toBeInstanceOf(PlaybackOperations.UnSkippablePeriodException.class);
    }
}
