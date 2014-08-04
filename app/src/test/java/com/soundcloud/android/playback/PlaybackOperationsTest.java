package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
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

import android.content.Intent;
import android.net.Uri;

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
    private TestEventBus eventBus = new TestEventBus();

    @Mock private ScModelManager modelManager;
    @Mock private TrackStorage trackStorage;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    @Before
    public void setUp() throws Exception {
        playbackOperations = new PlaybackOperations(Robolectric.application, modelManager, trackStorage,
                playQueueManager, eventBus, playSessionStateProvider);
        track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());
    }

    @Test
    public void playTrackShouldNotOpenPlayerActivity() {
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
     public void playTrackSetsPlayQueueOnPlayQueueManagerFromInitialTrack() {
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void playTrackOpensCurrentTrackThroughService() {
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playTrackShouldNotSendServiceIntentIfTrackAlreadyPlayingWithSameOrigin() {
        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).toBeNull();
    }

    @Test
    public void playTrackShouldSendServiceIntentIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());
        playbackOperations.playTrack(Robolectric.application, track, Screen.EXPLORE_TRENDING_AUDIO);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).not.toBeNull();
    }

    @Test
    public void playExploreTrackSetsPlayQueueAndOriginOnPlayQueueManager() {
        playbackOperations.playExploreTrack(Robolectric.application, track, EXPLORE_VERSION, ORIGIN_SCREEN.get());

        final PlaySessionSource expected = new PlaySessionSource(ORIGIN_SCREEN.get());
        expected.setExploreVersion(EXPLORE_VERSION);

        checkSetNewPlayQueueArgs(0, expected, track.getId());

    }

    @Test
    public void playExploreTrackPlaysCurrentTrackThroughService() throws Exception {
        playbackOperations.playExploreTrack(Robolectric.application, track, EXPLORE_VERSION, ORIGIN_SCREEN.get());

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playExploreTrackCallsFetchRelatedTracksOnPlayQueueManager() {
        playbackOperations.playExploreTrack(Robolectric.application, track, EXPLORE_VERSION, ORIGIN_SCREEN.get());

        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void playPlaylistFromPositionDoesNotOpenPlayerActivity() {
        final Observable<List<Long>> trackIdList = Observable.<List<Long>>just(Lists.newArrayList(track.getId()));
        when(trackStorage.getTrackIdsForUriAsync(any(Uri.class))).thenReturn(trackIdList);
        playbackOperations.playPlaylistFromPosition(Robolectric.application, playlist.toPropertySet(),
                Observable.just(track.getUrn()), track.getUrn(), 0, Screen.YOUR_LIKES);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
    public void playFromPlaylistSetsNewPlayqueueOnPlayQueueManagerFromPlaylist() throws CreateModelException {
        List<PublicApiTrack> tracks = TestHelper.createTracks(3);
        PublicApiPlaylist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final ArrayList<Long> trackIds = Lists.newArrayList(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(trackIds));

        playbackOperations.playPlaylistFromPosition(
                Robolectric.application,
                playlist.toPropertySet(),
                Observable.from(tracks.get(0).getUrn(), tracks.get(1).getUrn(), tracks.get(2).getUrn()),
                tracks.get(1).getUrn(),
                1,
                ORIGIN_SCREEN);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());

        checkSetNewPlayQueueArgs(1, playSessionSource, tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playFromPlaylistPlaysCurrentTrackThroughPlaybackService() throws CreateModelException {
        List<PublicApiTrack> tracks = TestHelper.createTracks(3);
        PublicApiPlaylist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final ArrayList<Long> trackIds = Lists.newArrayList(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(trackIds));

        playbackOperations.playPlaylistFromPosition(Robolectric.application, playlist.toPropertySet(),
                Observable.just(tracks.get(1).getUrn()), tracks.get(1).getUrn(), 1, ORIGIN_SCREEN);

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startsServiceIfPlayingQueueHasSameContextWithDifferentPlaylistSources() throws CreateModelException {
        List<PublicApiTrack> tracks = TestHelper.createTracks(3);
        PublicApiPlaylist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final ArrayList<Long> trackIds = Lists.newArrayList(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(trackIds));

        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get()); // same screen origin
        when(playQueueManager.isPlaylist()).thenReturn(true);
        when(playQueueManager.getPlaylistId()).thenReturn(playlist.getId() + 1); // different Playlist Id

        playbackOperations.playPlaylistFromPosition(Robolectric.application, playlist.toPropertySet(),
                Observable.just(tracks.get(1).getUrn()), tracks.get(1).getUrn(), 1, Screen.EXPLORE_TRENDING_MUSIC);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).not.toBeNull();
    }

    @Test
    public void shouldTogglePlayback() {
        playbackOperations.togglePlayback();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toBe(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION);
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
        final PlaybackProgress progress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        playbackOperations.previousTrack();

        verify(playQueueManager).moveToPreviousTrack();
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressEqualToTolerance() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

        playbackOperations.previousTrack();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(0L);
    }

    @Test
    public void previousTrackSeeksToZeroIfProgressGreaterThanTolerance() {
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3001L, 5000));

        playbackOperations.previousTrack();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(0L);
    }

    @Test
    public void previousTrackCallsPreviousTrackIfPlayingAudioAdWithProgressEqualToTimeout() {
        final PlaybackProgress progress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(PlaybackProgress.empty());

        playbackOperations.previousTrack();

        verify(playQueueManager).moveToPreviousTrack();
    }

    @Test
    public void previousTrackDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        final PlaybackProgress progress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        playbackOperations.previousTrack();

        verify(playQueueManager, never()).moveToPreviousTrack();
    }

    @Test
    public void nextTrackCallsNextTrackOnPlayQueueManager() {
        playbackOperations.nextTrack();

        verify(playQueueManager).nextTrack();
    }

    @Test
    public void nextTrackCallsNextTrackIfPlayingAudioAdWithProgressEqualToTimeout() {
        final PlaybackProgress progress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        playbackOperations.nextTrack();

        verify(playQueueManager).nextTrack();
    }

    @Test
    public void nextTrackDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        final PlaybackProgress progress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        playbackOperations.nextTrack();

        verify(playQueueManager, never()).nextTrack();
    }


    @Test
    public void seeksToProvidedPosition() {
        playbackOperations.seek(350L);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(350L);
    }

    @Test
    public void seekSeeksToProvidedPositionIfPlayingAudioAdWithProgressEqualTimeout() {
        final PlaybackProgress progress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        playbackOperations.seek(350L);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(350L);
    }

    @Test
    public void seekDoesNothingIfPlayingAudioAdWithProgressLessThanTimeout() {
        final PlaybackProgress progress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

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
        List<PublicApiTrack> tracks = TestHelper.createTracks(3);
        PublicApiPlaylist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        playbackOperations.playPlaylist(playlist, ORIGIN_SCREEN);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        checkSetNewPlayQueueArgs(0, playSessionSource, tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playPlaylistOpensCurrentTrackThroughService() throws CreateModelException {
        List<PublicApiTrack> tracks = TestHelper.createTracks(3);
        PublicApiPlaylist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        playbackOperations.playPlaylist(playlist, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playPlaylistFiresShowPlayerEvent() {
        playbackOperations.playPlaylist(playlist, ORIGIN_SCREEN);

        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.SHOW_PLAYER);
    }

    @Test
    public void playFromIdsShuffledSetsPlayQueueOnPlayQueueManagerWithGivenTrackIdList() {
        final ArrayList<Long> idsOrig = Lists.newArrayList(1L, 2L, 3L);
        playbackOperations.playFromIdListShuffled(idsOrig, Screen.YOUR_LIKES);

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
        final ArrayList<Long> idsOrig = Lists.newArrayList(1L, 2L, 3L);
        playbackOperations.playFromIdListShuffled(idsOrig, Screen.YOUR_LIKES);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playFromAdapterShouldRemoveDuplicates() throws Exception {
        ArrayList<PublicApiTrack> playables = Lists.newArrayList(new PublicApiTrack(1L), new PublicApiTrack(2L), new PublicApiTrack(3L), new PublicApiTrack(2L), new PublicApiTrack(1L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 4, null, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(2, new PlaySessionSource(ORIGIN_SCREEN.get()), 2L, 3L, 1L);
    }

    @Test
    public void playFromAdapterShouldNotOpenPlayerActivity() throws Exception {
        ArrayList<PublicApiTrack> playables = Lists.newArrayList(new PublicApiTrack(1L), new PublicApiTrack(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, Screen.SIDE_MENU_STREAM); // clicked 2nd track

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
    public void playFromAdapterSetsPlayQueueOnPlayQueueManagerFromListOfTracks() throws Exception {
        ArrayList<PublicApiTrack> playables = Lists.newArrayList(new PublicApiTrack(1L), new PublicApiTrack(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterOpensCurrentTrackThroughPlaybackService() throws Exception {
        ArrayList<PublicApiTrack> playables = Lists.newArrayList(new PublicApiTrack(1L), new PublicApiTrack(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playFromAdapterDoesNotIncludeNonTracksWhenSettingPlayQueue() throws Exception {
        List<Playable> playables = Lists.newArrayList(new PublicApiTrack(1L), playlist, new PublicApiTrack(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, null, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterWithUriShouldNotAdjustPlayPositionWhenPlayablesDoNotIncludePlaylists()  {
        final List<PublicApiTrack> playables = Lists.newArrayList(new PublicApiTrack(5L), new PublicApiTrack(1L), new PublicApiTrack(2L));
        final ArrayList<Long> trackIds = Lists.newArrayList(5L, 1L, 2L);

        when(trackStorage.getTrackIdsForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.<List<Long>>just(trackIds));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, Content.ME_LIKES.uri, ORIGIN_SCREEN);

        checkSetNewPlayQueueArgs(2, new PlaySessionSource(ORIGIN_SCREEN.get()), 5L, 1L, 2L);
    }

    @Test
    public void playFromAdapterWithUriShouldAdjustPlayPositionWhenPlayablesIncludePlaylists()  {
        final List<Playable> playables = Lists.newArrayList(new PublicApiTrack(1L), playlist, new PublicApiTrack(2L));
        final ArrayList<Long> trackIds = Lists.newArrayList(1L, 2L);

        when(trackStorage.getTrackIdsForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.<List<Long>>just(trackIds));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, Content.ME_LIKES.uri, ORIGIN_SCREEN);

        checkSetNewPlayQueueArgs(1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterShouldFallBackToPositionZeroIfInitialItemNotFound()  {
        final ArrayList<PublicApiTrack> playables = Lists.newArrayList(new PublicApiTrack(1L), new PublicApiTrack(2L));
        final ArrayList<Long> ids = Lists.newArrayList(6L, 7L);

        when(trackStorage.getTrackIdsForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.<List<Long>>just(ids));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, Content.ME_LIKES.uri, ORIGIN_SCREEN);

        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), 6L, 7L);
    }

    @Test
    public void playFromAdapterShouldStartPlaylistActivity() throws Exception {
        List<Playable> playables = Lists.newArrayList(new PublicApiTrack(1L), playlist, new PublicApiTrack(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYLIST);
        expect(startedActivity.getParcelableExtra(PublicApiPlaylist.EXTRA_URN)).toEqual(playlist.getUrn());
    }

    @Test(expected = AssertionError.class)
    public void playFromAdapterShouldThrowAssertionErrorWhenPositionGreaterThanSize() throws Exception {
        List<Playable> playables = Lists.<Playable>newArrayList(track);
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, Content.ME_LIKES.uri, ORIGIN_SCREEN);
    }

    @Test
    public void startPlaybackWithRecommendationsCachesTrack() throws Exception {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        verify(modelManager).cache(track);
    }

    @Test
    public void startPlaybackWithRecommendationsSetsConfiguredPlayQueueOnPlayQueueManager() throws Exception {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void startPlaybackWithRecommendationsOpensCurrentThroughPlaybackService() throws Exception {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startPlaybackWithRecommendationsByTrackCallsFetchRecommendationsOnPlayQueueManager() throws Exception {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void startPlaybackWithRecommendationsByIdSetsPlayQueueOnPlayQueueManager() throws Exception {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), 123L);
    }

    @Test
    public void startPlaybackWithRecommendationsByIdOpensCurrentThroughPlaybackService() throws Exception {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startPlaybackWithRecommendationsByIdCallsFetchRelatedOnPlayQueueManager() throws Exception {
        playbackOperations.startPlaybackWithRecommendations(TRACK_URN, ORIGIN_SCREEN);
        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void getUpIntentShouldReturnNullWithNoOriginScreen() throws Exception {
        when(playQueueManager.getScreenTag()).thenReturn(null);
        expect(playbackOperations.getServiceBasedUpIntent()).toBeNull();
    }

    @Test
    public void getUpIntentShouldReturnPlaylistUpIntent() throws Exception {
        when(playQueueManager.getScreenTag()).thenReturn(Screen.PLAYLIST_DETAILS.get());
        when(playQueueManager.isPlaylist()).thenReturn(true);
        when(playQueueManager.getPlaylistId()).thenReturn(123L);
        final Intent intent = playbackOperations.getServiceBasedUpIntent();
        expect(intent).toHaveAction("com.soundcloud.android.action.PLAYLIST");
        expect(intent.getParcelableExtra(PublicApiPlaylist.EXTRA_URN)).toEqual(Urn.forPlaylist(123L));
        expect(intent).toHaveFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        expect(intent.getIntExtra("ScreenOrdinal", -1)).toEqual(Screen.PLAYLIST_DETAILS.ordinal());
    }

    @Test
    public void getUpIntentShouldReturnLikeUpIntent() throws Exception {
        when(playQueueManager.getScreenTag()).thenReturn(Screen.SIDE_MENU_LIKES.get());
        when(playQueueManager.isPlaylist()).thenReturn(false);
        final Intent intent = playbackOperations.getServiceBasedUpIntent();
        expect(intent).toHaveAction("com.soundcloud.android.action.LIKES");
        expect(intent).toHaveFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Test
    public void playTracksShouldNotOpenLegacyPlayerIfVisualPlayerEnabled() {
        final Observable<TrackUrn> tracks = Observable.just(Urn.forTrack(123));
        playbackOperations.playTracks(Robolectric.application, Urn.forTrack(123), tracks, 0, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();
        expect(startedActivity).toBeNull();
    }

    private void checkSetNewPlayQueueArgs(int startPosition, PlaySessionSource playSessionSource, Long... ids){
        verify(playQueueManager).setNewPlayQueue(
                eq(PlayQueue.fromIdList(Lists.newArrayList(ids), playSessionSource)), eq(startPosition),
                eq(playSessionSource));
    }

    protected void checkLastStartedServiceForPlayCurrentAction() {
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toEqual(PlaybackService.Actions.PLAY_CURRENT);
    }
}
