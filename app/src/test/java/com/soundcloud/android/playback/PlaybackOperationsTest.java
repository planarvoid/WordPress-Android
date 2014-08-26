package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.createNewUserPlaylist;
import static com.soundcloud.android.robolectric.TestHelper.createTracks;
import static com.soundcloud.android.robolectric.TestHelper.createTracksUrn;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.ui.view.PlaybackToastViewController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackUrn;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Action1;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackOperationsTest {

    private static final String EXPLORE_VERSION = "explore-version";
    private static final Screen ORIGIN_SCREEN = Screen.EXPLORE_TRENDING_MUSIC;
    public static final TrackUrn TRACK_URN = Urn.forTrack(123L);

    private PlaybackOperations playbackOperations;

    private PublicApiTrack track;
    private PublicApiPlaylist playlist;

    @Mock private ScModelManager modelManager;
    @Mock private TrackStorage trackStorage;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlaybackToastViewController playbackToastViewController;
    private final Action1<List<TrackUrn>> emptyAction = rx.functions.Actions.empty();

    @Before
    public void setUp() throws Exception {
        playbackOperations = new PlaybackOperations(Robolectric.application, modelManager, trackStorage,
                playQueueManager, playSessionStateProvider, playbackToastViewController);
        track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());
    }

    @Test
     public void playTrackSetsPlayQueueOnPlayQueueManagerFromInitialTrack() {
        playbackOperations.playTrack(track.getUrn(), new PlaySessionSource(ORIGIN_SCREEN));
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void playTrackOpensCurrentTrackThroughService() {
        playbackOperations.playTrack(track.getUrn(), new PlaySessionSource(ORIGIN_SCREEN));

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playTrackShouldNotSendServiceIntentIfTrackAlreadyPlayingWithSameOrigin() {
        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);
        playbackOperations.playTrack(track.getUrn(), new PlaySessionSource(ORIGIN_SCREEN));

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).toBeNull();
    }

    @Test
    public void playTrackShouldSendServiceIntentIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());
        playbackOperations.playTrack(track.getUrn(), new PlaySessionSource(Screen.EXPLORE_TRENDING_AUDIO));

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).not.toBeNull();
    }

    @Test
    public void playExploreTrackSetsPlayQueueAndOriginOnPlayQueueManager() {
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setExploreVersion(EXPLORE_VERSION);
        playbackOperations.playTrackWithRecommendations(track.getUrn(), playSessionSource);

        final PlaySessionSource expected = new PlaySessionSource(ORIGIN_SCREEN.get());
        expected.setExploreVersion(EXPLORE_VERSION);

        checkSetNewPlayQueueArgs(0, expected, track.getId());
    }

    @Test
    public void playExploreTrackPlaysCurrentTrackThroughService() {
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setExploreVersion(EXPLORE_VERSION);
        playbackOperations.playTrackWithRecommendations(track.getUrn(), playSessionSource);

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playExploreTrackCallsFetchRelatedTracksOnPlayQueueManager() {
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setExploreVersion(EXPLORE_VERSION);
        playbackOperations.playTrackWithRecommendations(track.getUrn(), playSessionSource);

        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void playFromPlaylistSetsNewPlayqueueOnPlayQueueManagerFromPlaylist() throws CreateModelException {
        List<PublicApiTrack> tracks = createTracks(3);
        PublicApiPlaylist playlist = createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final List<TrackUrn> trackUrns = createTracksUrn(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getUrn().numericId, playlist.getUserId());
        playbackOperations.playTracks(tracks.get(1).getUrn(), Observable.from(tracks.get(0).getUrn(), tracks.get(1).getUrn(), tracks.get(2).getUrn()), 1, playSessionSource, emptyAction);

        checkSetNewPlayQueueArgs(1, playSessionSource, tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playFromPlaylistPlaysCurrentTrackThroughPlaybackService() throws CreateModelException {
        List<PublicApiTrack> tracks = createTracks(3);
        PublicApiPlaylist playlist = createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final List<TrackUrn> trackUrns = createTracksUrn(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getUrn().numericId, playlist.getUserId());
        playbackOperations.playTracks(tracks.get(1).getUrn(), Observable.just(tracks.get(1).getUrn()), 1, playSessionSource, emptyAction);

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startsServiceIfPlayingQueueHasSameContextWithDifferentPlaylistSources() throws CreateModelException {
        List<PublicApiTrack> tracks = createTracks(3);
        PublicApiPlaylist playlist = createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final List<TrackUrn> trackUrns = createTracksUrn(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(trackUrns));

        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get()); // same screen origin
        when(playQueueManager.isPlaylist()).thenReturn(true);
        when(playQueueManager.getPlaylistId()).thenReturn(playlist.getId() + 1); // different Playlist Id

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.EXPLORE_TRENDING_MUSIC.get());
        playSessionSource.setPlaylist(playlist.getUrn().numericId, playlist.getUserId());
        expect(playbackOperations.playTracks(tracks.get(1).getUrn(), Observable.just(tracks.get(1).getUrn()), 1, playSessionSource, emptyAction)).toBeTrue();

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

        expect(playbackOperations.playTracks(TRACK_URN, Observable.just(TRACK_URN), 1, playSessionSource, emptyAction)).toBeFalse();
    }

    @Test
    public void playTracksNoOpsWhenItIsPQMCurrentTrackAndCurrentScreenSourceAndCurrentPlaylist()  {
        final long playlistId = 456L;
        final long playlistOwnerId = 789L;
        final String screen = "origin_screen";
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setPlaylist(playlistId, playlistOwnerId);
        when(playQueueManager.getScreenTag()).thenReturn(screen);
        when(playQueueManager.getPlaylistId()).thenReturn(playlistId);
        when(playQueueManager.isPlaylist()).thenReturn(true);
        when(playQueueManager.isCurrentTrack(TRACK_URN)).thenReturn(true);
        when(playQueueManager.isCurrentPlaylist(playlistId)).thenReturn(true);

        expect(playbackOperations.playTracks(TRACK_URN, Observable.just(TRACK_URN), 1, playSessionSource, emptyAction)).toBeFalse();
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
    public void playPlaylistSetsPlayQueueOnPlayQueueManagerFromPlaylist() throws CreateModelException {
        List<PublicApiTrack> tracks = createTracks(3);
        PublicApiPlaylist playlist = createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final PlaySessionSource playSessionSource1 = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource1.setPlaylist(playlist.getId(), playlist.getUserId());
        playbackOperations.playPlaylist(playlist, playSessionSource1);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        checkSetNewPlayQueueArgs(0, playSessionSource, tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playPlaylistOpensCurrentTrackThroughService() throws CreateModelException {
        List<PublicApiTrack> tracks = createTracks(3);
        PublicApiPlaylist playlist = createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playbackOperations.playPlaylist(playlist, playSessionSource);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playFromIdsShuffledSetsPlayQueueOnPlayQueueManagerWithGivenTrackIdList() {
        final List<TrackUrn> idsOrig = createTracksUrn(1L, 2L, 3L);
        playbackOperations.playTracksShuffled(idsOrig, new PlaySessionSource(Screen.YOUR_LIKES));

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
        final List<TrackUrn> idsOrig = createTracksUrn(1L, 2L, 3L);
        playbackOperations.playTracksShuffled(idsOrig, new PlaySessionSource(Screen.YOUR_LIKES));
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playTracksWithTrackListContainsTracksOpensCurrentTrackThroughPlaybackService() {
        final Observable<TrackUrn> tracks = Observable.just(Urn.forTrack(123L));
        playbackOperations.playTracks(TRACK_URN, tracks, 2, new PlaySessionSource(ORIGIN_SCREEN), emptyAction);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playTracksWithEmptyTrackListDoesNotOpenCurrentTrackThroughPlaybackService() {
        Observable<TrackUrn> tracks = Observable.empty();
        playbackOperations.playTracks(TRACK_URN, tracks, 2, new PlaySessionSource(ORIGIN_SCREEN), emptyAction);
        expectLastStartedServiceToBeNull();
    }

    @Test
    public void playFromAdapterShouldRemoveDuplicates() throws Exception {
        List<TrackUrn> playables = createTracksUrn(1L, 2L, 3L, 2L, 1L);

        playbackOperations.playTracks(playables, 4, new PlaySessionSource(ORIGIN_SCREEN));
        checkSetNewPlayQueueArgs(2, new PlaySessionSource(ORIGIN_SCREEN.get()), 2L, 3L, 1L);
    }

    @Test
    public void playFromAdapterSetsPlayQueueOnPlayQueueManagerFromListOfTracks() throws Exception {
        List<TrackUrn> playables = createTracksUrn(1L, 2L);
        playbackOperations.playTracks(playables, 1, new PlaySessionSource(ORIGIN_SCREEN));

        checkSetNewPlayQueueArgs(1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterOpensCurrentTrackThroughPlaybackService() throws Exception {
        List<TrackUrn> playables = createTracksUrn(1L, 2L);

        playbackOperations.playTracks(playables, 1, new PlaySessionSource(ORIGIN_SCREEN));

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playFromAdapterShouldFallBackToPositionZeroIfInitialItemNotFound()  {
        final List<TrackUrn> playables = createTracksUrn(1L, 2L);
        final List<TrackUrn> ids = createTracksUrn(6L, 7L);

        when(trackStorage.getTracksForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.just(ids));
        TrackUrn initialTrack = playables.get(1);
        playbackOperations.playFromUri(Content.ME_LIKES.uri, 1, initialTrack, new PlaySessionSource(ORIGIN_SCREEN), emptyAction);


        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), 6L, 7L);
    }

    @Test
    public void startPlaybackWithRecommendationsCachesTrack() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        verify(modelManager).cache(track);
    }

    @Test
    public void startPlaybackWithRecommendationsSetsConfiguredPlayQueueOnPlayQueueManager() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void startPlaybackWithRecommendationsOpensCurrentThroughPlaybackService() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startPlaybackWithRecommendationsByTrackCallsFetchRecommendationsOnPlayQueueManager() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void startPlaybackWithRecommendationsByIdSetsPlayQueueOnPlayQueueManager() {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), 123L);
    }

    @Test
    public void startPlaybackWithRecommendationsByIdOpensCurrentThroughPlaybackService() {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startPlaybackWithRecommendationsByIdCallsFetchRelatedOnPlayQueueManager() {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN);
        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayTrack() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.playTrack(track.getUrn(), new PlaySessionSource(ORIGIN_SCREEN));

        expectUnskippableToastAndNoNewPlayQueueSet();
    }

    @Test
    public void allowSkippingWhenAdIsSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());

        playbackOperations.playTrack(track.getUrn(), new PlaySessionSource(ORIGIN_SCREEN));

        checkSetNewPlayQueueArgs(0, playSessionSource, track.getId());
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayFromAdapter() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        final List<TrackUrn> tracks = createTracksUrn(1L);
        final List<TrackUrn> trackUrns = createTracksUrn(1L);
        when(trackStorage.getTracksForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.just(trackUrns));

        TrackUrn initialTrack = tracks.get(0);
        playbackOperations.playFromUri(Content.ME_LIKES.uri, 0, initialTrack, new PlaySessionSource(ORIGIN_SCREEN), emptyAction);

        expectUnskippableToastAndNoNewPlayQueueSet();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayFromShuffledIds() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        final List<TrackUrn> trackUrns = createTracksUrn(1L);

        playbackOperations.playTracksShuffled(trackUrns, new PlaySessionSource(Screen.YOUR_LIKES));

        expectUnskippableToastAndNoNewPlayQueueSet();

    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayPlaylist() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        playbackOperations.playPlaylist(playlist, playSessionSource);

        expectUnskippableToastAndNoNewPlayQueueSet();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnPlayPlaylistFromPosition() throws CreateModelException {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        when(trackStorage.getTracksForUriAsync(playlist.toUri())).thenReturn(Observable.just(createTracksUrn(123L)));

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getUrn().numericId, playlist.getUserId());
        playbackOperations.playTracks(TRACK_URN, Observable.just(TRACK_URN), 0, playSessionSource, emptyAction);

        expectUnskippableToastAndNoNewPlayQueueSet();
    }

    @Test
    public void showUnskippableToastWhenAdIsPlayingOnRecommendations() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);

        expectUnskippableToastAndNoNewPlayQueueSet();
    }

    private void setupAdInProgress(long currentProgress) {
        final PlaybackProgress progress = new PlaybackProgress(currentProgress, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
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

    private void expectUnskippableToastAndNoNewPlayQueueSet() {
        verify(playbackToastViewController).showUnkippableAdToast();
        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), anyInt(), any(PlaySessionSource.class));
    }
}
