package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
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
import com.xtremelabs.robolectric.shadows.ShadowToast;
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
     public void playTrackSetsPlayQueueOnPlayQueueManagerFromInitialTrack() {
        playbackOperations.playTrack(track, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void playTrackOpensCurrentTrackThroughService() {
        playbackOperations.playTrack(track, ORIGIN_SCREEN);

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playTrackShouldNotSendServiceIntentIfTrackAlreadyPlayingWithSameOrigin() {
        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);
        playbackOperations.playTrack(track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).toBeNull();
    }

    @Test
    public void playTrackShouldSendServiceIntentIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get());
        playbackOperations.playTrack(track, Screen.EXPLORE_TRENDING_AUDIO);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).not.toBeNull();
    }

    @Test
    public void playExploreTrackSetsPlayQueueAndOriginOnPlayQueueManager() {
        playbackOperations.playExploreTrack(track, EXPLORE_VERSION, ORIGIN_SCREEN.get());

        final PlaySessionSource expected = new PlaySessionSource(ORIGIN_SCREEN.get());
        expected.setExploreVersion(EXPLORE_VERSION);

        checkSetNewPlayQueueArgs(0, expected, track.getId());
    }

    @Test
    public void playExploreTrackPlaysCurrentTrackThroughService() {
        playbackOperations.playExploreTrack(track, EXPLORE_VERSION, ORIGIN_SCREEN.get());

        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playExploreTrackCallsFetchRelatedTracksOnPlayQueueManager() {
        playbackOperations.playExploreTrack(track, EXPLORE_VERSION, ORIGIN_SCREEN.get());

        verify(playQueueManager).fetchTracksRelatedToCurrentTrack();
    }

    @Test
    public void playFromPlaylistSetsNewPlayqueueOnPlayQueueManagerFromPlaylist() throws CreateModelException {
        List<PublicApiTrack> tracks = TestHelper.createTracks(3);
        PublicApiPlaylist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final ArrayList<Long> trackIds = Lists.newArrayList(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(trackIds));

        playbackOperations.playPlaylistFromPosition(
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

        playbackOperations.playPlaylistFromPosition(playlist.toPropertySet(),
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

        playbackOperations.playPlaylistFromPosition(playlist.toPropertySet(),
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
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(3000L, 5000));

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
    public void previousTrackShowsToastWhenPlaybackNotSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.previousTrack();

        expectUnskippableToastMessage();
    }

    @Test
    public void nextTrackShowsToastWhenPlaybackNotSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.nextTrack();

        expectUnskippableToastMessage();
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
    public void seeksToProvidedPosition() {
        playbackOperations.seek(350L);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(350L);
    }

    @Test
    public void seekSeeksToProvidedPositionIfPlayingAudioAdWithProgressEqualTimeout() {
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
    public void playFromAdapterShouldStartPlaylistActivity() {
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
    public void getUpIntentShouldReturnNullWithNoOriginScreen() {
        when(playQueueManager.getScreenTag()).thenReturn(null);
        expect(playbackOperations.getServiceBasedUpIntent()).toBeNull();
    }

    @Test
    public void getUpIntentShouldReturnPlaylistUpIntent() {
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
    public void getUpIntentShouldReturnLikeUpIntent() {
        when(playQueueManager.getScreenTag()).thenReturn(Screen.SIDE_MENU_LIKES.get());
        when(playQueueManager.isPlaylist()).thenReturn(false);
        final Intent intent = playbackOperations.getServiceBasedUpIntent();
        expect(intent).toHaveAction("com.soundcloud.android.action.LIKES");
        expect(intent).toHaveFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Test
    public void playTracksShouldNotOpenLegacyPlayerIfVisualPlayerEnabled() {
        final Observable<TrackUrn> tracks = Observable.just(Urn.forTrack(123));
        playbackOperations.playTracks(Urn.forTrack(123), tracks, 0, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();
        expect(startedActivity).toBeNull();
    }

    @Test
    public void sendPlayerUIUnskippableWhenAdIsPlayingOnPlayTrack() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.playTrack(track, ORIGIN_SCREEN);

        expectUnskippablePlaybackToastMessageAndNoNewPlayQueueSet();
    }

    @Test
    public void allowSkippingWhenAdIsSkippable() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1);
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());

        playbackOperations.playTrack(track, ORIGIN_SCREEN);

        checkSetNewPlayQueueArgs(0, playSessionSource, track.getId());
    }

    @Test
    public void sendPlayerUIUnskippableWhenAdIsPlayingOnPlayFromAdapter() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        final ArrayList<PublicApiTrack> tracks = Lists.newArrayList(new PublicApiTrack(1L));
        final ArrayList<Long> trackIds = Lists.newArrayList(1L);
        when(trackStorage.getTrackIdsForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.<List<Long>>just(trackIds));

        playbackOperations.playFromAdapter(Robolectric.application, tracks, 0, Content.ME_LIKES.uri, ORIGIN_SCREEN);

        expectUnskippablePlaybackToastMessageAndNoNewPlayQueueSet();
    }

    @Test
    public void sendPlayerUIUnskippableWhenAdIsPlayingOnPlayFromShuffledIds() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        final ArrayList<Long> idsOrig = Lists.newArrayList(1L);

        playbackOperations.playFromIdListShuffled(idsOrig, Screen.YOUR_LIKES);

        expectUnskippablePlaybackToastMessageAndNoNewPlayQueueSet();
    }

    @Test
    public void sendPlayerUIUnskippableWhenAdIsPlayingOnPlayPlaylist() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.playPlaylist(playlist, ORIGIN_SCREEN);

        expectUnskippablePlaybackToastMessageAndNoNewPlayQueueSet();
    }

    @Test
    public void sendPlayerUIUnskippableWhenAdIsPlayingOnPlayPlaylistFromPosition() throws CreateModelException {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(Lists.newArrayList(123L)));

        playbackOperations.playPlaylistFromPosition(playlist.toPropertySet(),
                Observable.just(TRACK_URN), TRACK_URN, 0, ORIGIN_SCREEN);

        expectUnskippablePlaybackToastMessageAndNoNewPlayQueueSet();
    }

    @Test
    public void sendPlayerUIUnskippableWhenAdIsPlayingOnRecommendations() {
        setupAdInProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1);

        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);

        expectUnskippablePlaybackToastMessageAndNoNewPlayQueueSet();
    }

    @Test
    public void sendsExpandPlayerEventWhenQueueNotChangedFromIdList() {
        ArrayList<PublicApiTrack> playables = Lists.newArrayList(new PublicApiTrack(1L), new PublicApiTrack(2L));
        when(playQueueManager.isCurrentTrack(Urn.forTrack(2L))).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);

        expect(eventBus.lastEventOn(EventQueue.PLAYER_UI).getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void sendsExpandPlayerEventWhenQueueNotChangedFromUri() {
        Uri uri = Uri.parse("some:uri");
        ArrayList<PublicApiTrack> playables = Lists.newArrayList(new PublicApiTrack(1L), new PublicApiTrack(2L));
        when(playQueueManager.isCurrentTrack(Urn.forTrack(2L))).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, uri, ORIGIN_SCREEN);

        expect(eventBus.lastEventOn(EventQueue.PLAYER_UI).getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void sendsExpandPlayerEventWhenQueueNotChangedFromTracks() {
        final Observable<TrackUrn> tracks = Observable.just(Urn.forTrack(123L));
        when(playQueueManager.isCurrentTrack(Urn.forTrack(123L))).thenReturn(true);
        when(playQueueManager.getScreenTag()).thenReturn(ORIGIN_SCREEN.get());

        playbackOperations.playTracks(Urn.forTrack(123L), tracks, 0, ORIGIN_SCREEN);

        expect(eventBus.lastEventOn(EventQueue.PLAYER_UI).getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    private void setupAdInProgress(long currentProgress) {
        final PlaybackProgress progress = new PlaybackProgress(currentProgress, 30000);
        when(playSessionStateProvider.getCurrentPlayQueueTrackProgress()).thenReturn(progress);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
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

    private void expectUnskippablePlaybackToastMessageAndNoNewPlayQueueSet() {
        expectUnskippableToastMessage();
        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), anyInt(), any(PlaySessionSource.class));
    }

    private void expectUnskippableToastMessage() {
        expect(ShadowToast.getTextOfLatestToast()).toEqual(Robolectric.application.getString(R.string.ad_in_progress));
    }

}
