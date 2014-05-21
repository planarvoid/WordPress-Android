package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.http.APIResponse;
import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.api.Token;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.content.Intent;
import android.net.Uri;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackOperationsTest {

    private static final String EXPLORE_VERSION = "explore-version";
    private static final Screen ORIGIN_SCREEN = Screen.EXPLORE_TRENDING_MUSIC;

    private PlaybackOperations playbackOperations;
    private Track track;

    @Mock
    private ScModelManager modelManager;
    @Mock
    private TrackStorage trackStorage;
    @Mock
    private Observer observer;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private HttpProperties httpProperties;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private FeatureFlags featureFlags;
    @Mock
    private Token token;
    @Mock
    private EventBus eventBus;
    @Mock
    private RxHttpClient rxHttpClient;

    @Before
    public void setUp() throws Exception {
        playbackOperations = new PlaybackOperations(Robolectric.application, modelManager, trackStorage, playQueueManager,
                accountOperations, httpProperties, rxHttpClient, featureFlags, eventBus);
        track = TestHelper.getModelFactory().createModel(Track.class);
    }

    @Test
    public void playTrackShouldOpenPlayerActivityWithInitialTrackIdIfVisualPlayerDisabled() {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(false);
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(track.getId());
    }

    @Test
    public void playTrackShouldNotOpenPlayerActivity() {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
    public void playTrackFiresPlayTriggeredEvent() {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        PlayerUIEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
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
        when(playQueueManager.getCurrentTrackId()).thenReturn(track.getId());
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService()).not.toBeNull();
    }

    @Test
    public void playTrackShouldSendServiceIntentIfTrackAlreadyPlayingWithDifferentContext() {
        when(playQueueManager.getCurrentTrackId()).thenReturn(track.getId());
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

        verify(playQueueManager).fetchRelatedTracks(track.getId());
    }

    @Test
    public void playPlaylistFromPositionOpensPlayerActivityWithInitialTrackIdIfVisualPlayerDisabled() {
        Playlist playlist = new Playlist(3L);
        final Observable<List<Long>> trackIdList = Observable.<List<Long>>just(Lists.newArrayList(track.getId()));
        when(trackStorage.getTrackIdsForUriAsync(any(Uri.class))).thenReturn(trackIdList);
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(false);

        playbackOperations.playPlaylistFromPosition(Robolectric.application, playlist, 0, track, Screen.YOUR_LIKES);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(track.getId());
    }

    @Test
    public void playPlaylistFromPositionDoesNotOpenPlayerActivity() {
        final Observable<List<Long>> trackIdList = Observable.<List<Long>>just(Lists.newArrayList(track.getId()));
        when(trackStorage.getTrackIdsForUriAsync(any(Uri.class))).thenReturn(trackIdList);
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);
        playbackOperations.playPlaylistFromPosition(Robolectric.application, new Playlist(3L), 0, track, Screen.YOUR_LIKES);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
    public void playPlaylistFromPositionFiresPlayTriggeredEvent() {
        final Observable<List<Long>> trackIdList = Observable.<List<Long>>just(Lists.newArrayList(track.getId()));
        when(trackStorage.getTrackIdsForUriAsync(any(Uri.class))).thenReturn(trackIdList);
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);
        playbackOperations.playPlaylistFromPosition(Robolectric.application, new Playlist(3L), 0, track, Screen.YOUR_LIKES);

        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        PlayerUIEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void playFromPlaylistSetsNewPlayqueueOnPlayQueueManagerFromPlaylist() throws CreateModelException {
        List<Track> tracks = TestHelper.createTracks(3);
        Playlist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final ArrayList<Long> trackIds = Lists.newArrayList(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(trackIds));

        playbackOperations.playPlaylistFromPosition(Robolectric.application, playlist, 1, tracks.get(1), ORIGIN_SCREEN);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist);

        checkSetNewPlayQueueArgs(1, playSessionSource, tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playFromPlaylistPlaysCurrentTrackThroughPlaybackService() throws CreateModelException {
        List<Track> tracks = TestHelper.createTracks(3);
        Playlist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final ArrayList<Long> trackIds = Lists.newArrayList(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(trackIds));

        playbackOperations.playPlaylistFromPosition(Robolectric.application, playlist, 1, tracks.get(1), ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startsServiceIfPlayingQueueHasSameContextWithDifferentPlaylistSources() throws CreateModelException {
        List<Track> tracks = TestHelper.createTracks(3);
        Playlist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final ArrayList<Long> trackIds = Lists.newArrayList(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(trackIds));

        when(playQueueManager.getCurrentTrackId()).thenReturn(tracks.get(1).getId()); // same track
        when(playQueueManager.getScreenTag()).thenReturn(Screen.EXPLORE_TRENDING_MUSIC.get()); // same screen origin
        when(playQueueManager.getPlaylistId()).thenReturn(-1L); // different Playlist Id

        playbackOperations.playPlaylistFromPosition(Robolectric.application, playlist, 1, tracks.get(1), Screen.EXPLORE_TRENDING_MUSIC);

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
    public void shouldPerformPreviousAction() {
        playbackOperations.previousTrack();

        verify(playQueueManager).previousTrack();
    }

    @Test
    public void shouldPerformNextAction() {
        playbackOperations.nextTrack();

        verify(playQueueManager).nextTrack();
    }

    @Test
    public void playPlaylistSetsPlayQueueOnPlayQueueManagerFromPlaylist() throws CreateModelException {
        List<Track> tracks = TestHelper.createTracks(3);
        Playlist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        playbackOperations.playPlaylist(playlist, ORIGIN_SCREEN);

        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist);
        checkSetNewPlayQueueArgs(0, playSessionSource, tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playPlaylistOpensCurrentTrackThroughService() throws CreateModelException {
        List<Track> tracks = TestHelper.createTracks(3);
        Playlist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        playbackOperations.playPlaylist(playlist, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playFromIdsShuffledSetsPlayQueueOnPlayQueueManagerWithGivenTrackIdList() {
        final ArrayList<Long> idsOrig = Lists.newArrayList(1L, 2L, 3L);
        playbackOperations.playFromIdListShuffled(Robolectric.application, idsOrig, Screen.YOUR_LIKES);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);

        ArgumentCaptor<PlayQueue> playQueueCaptor = ArgumentCaptor.forClass(PlayQueue.class);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.YOUR_LIKES);

        verify(playQueueManager).setNewPlayQueue(playQueueCaptor.capture(), eq(playSessionSource));

        final PlayQueue value = playQueueCaptor.getValue();
        expect(Lists.transform(value.getItems(), new Function<PlayQueueItem, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable PlayQueueItem input) {
                return input.getTrackId();
            }
        })).toContainExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    public void playFromIdsShuffledOpensCurrentTrackThroughPlaybackService() {
        final ArrayList<Long> idsOrig = Lists.newArrayList(1L, 2L, 3L);
        playbackOperations.playFromIdListShuffled(Robolectric.application, idsOrig, Screen.YOUR_LIKES);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playFromAdapterShouldRemoveDuplicates() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L), new Track(3L), new Track(2L), new Track(1L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 4, null, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs( 2, new PlaySessionSource(ORIGIN_SCREEN.get()), 2L, 3L, 1L);
    }

    @Test
    public void playFromAdapterShouldOpenPlayerActivityWithInitialTrackFromPositionIfVisualPlayerDisabled() throws Exception {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(false);

        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, Screen.SIDE_MENU_STREAM); // clicked 2nd track

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(2L);
    }

    @Test
    public void playFromAdapterShouldNotOpenPlayerActivity() throws Exception {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);

        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, Screen.SIDE_MENU_STREAM); // clicked 2nd track

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
    public void playFromAdapterFiresPlayTriggeredEvent() throws Exception {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);

        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, Screen.SIDE_MENU_STREAM); // clicked 2nd track

        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        PlayerUIEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void playFromAdapterSetsPlayQueueOnPlayQueueManagerFromListOfTracks() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterOpensCurrentTrackThroughPlaybackService() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void playFromAdapterDoesNotIncludeNonTracksWhenSettingPlayQueue() throws Exception {
        List<Playable> playables = Lists.newArrayList(new Track(1L), new Playlist(), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, null, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterWithUriShouldAdjustPlayPositionWithUpdatedContent()  {
        final List<Playable> playables = Lists.newArrayList(new Track(1L), new Playlist(), new Track(2L));
        final ArrayList<Long> value = Lists.newArrayList(5L, 1L, 2L);

        when(trackStorage.getTrackIdsForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.<List<Long>>just(value));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, Content.ME_LIKES.uri, ORIGIN_SCREEN);

        checkSetNewPlayQueueArgs(2,  new PlaySessionSource(ORIGIN_SCREEN.get()), 5L, 1L, 2L);
    }


    @Test
    public void playFromAdapterShouldStartPlaylistActivity() throws Exception {
        final Playlist playlist = new Playlist(1L);
        List<Playable> playables = Lists.newArrayList(new Track(1L), playlist, new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYLIST);
        expect(startedActivity.getParcelableExtra(Playlist.EXTRA_URN)).toEqual(playlist.getUrn());
    }

    @Test(expected = AssertionError.class)
    public void playFromAdapterShouldThrowAssertionErrorWhenPositionGreaterThanSize() throws Exception {
        List<Playable> playables = Lists.<Playable>newArrayList(track);
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, Content.ME_LIKES.uri, ORIGIN_SCREEN);
    }

    @Test
    public void startPlaybackWithRecommendationsCachesTrack() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        verify(modelManager).cache(track);
    }

    @Test
    public void startPlaybackWithRecommendationsSetsConfiguredPlayQueueOnPlayQueueManager() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void startPlaybackWithRecommendationsOpensCurrentThroughPlaybackService() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startPlaybackWithRecommendationsByTrackCallsFetchRecommendationsOnPlayQueueManager() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        playbackOperations.startPlaybackWithRecommendations(track, ORIGIN_SCREEN);
        verify(playQueueManager).fetchRelatedTracks(track.getId());
    }

    @Test
    public void startPlaybackWithRecommendationsByIdSetsPlayQueueOnPlayQueueManager() throws Exception {
        playbackOperations.startPlaybackWithRecommendations(123L, ORIGIN_SCREEN);
        checkSetNewPlayQueueArgs(0, new PlaySessionSource(ORIGIN_SCREEN.get()), 123L);
    }

    @Test
    public void startPlaybackWithRecommendationsByIdOpensCurrentThroughPlaybackService() throws Exception {
        playbackOperations.startPlaybackWithRecommendations(123L, ORIGIN_SCREEN);
        checkLastStartedServiceForPlayCurrentAction();
    }

    @Test
    public void startPlaybackWithRecommendationsByIdCallsFetchRelatedOnPlayQueueManager() throws Exception {
        playbackOperations.startPlaybackWithRecommendations(123L, ORIGIN_SCREEN);
        verify(playQueueManager).fetchRelatedTracks(123L);
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
        expect(intent.getParcelableExtra(Playlist.EXTRA_URN)).toEqual(Urn.forPlaylist(123L));
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

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenBuilding(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        playbackOperations.buildHLSUrlForTrack(track);
    }

    @Test
    public void shouldBuildHLSUrlForTrackBasedOnTrackURN() {
        Track mockTrack = mock(Track.class);
        when(mockTrack.getUrn()).thenReturn(Urn.forTrack(123));
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getSoundCloudToken()).thenReturn(token);
        token.access = "access";
        when(httpProperties.getPrivateApiHostWithHttpScheme()).thenReturn("https://somehost/path");

        expect(playbackOperations
                .buildHLSUrlForTrack(mockTrack))
                .toEqual("https://somehost/path/tracks/soundcloud:sounds:123/streams/hls?oauth_token=access");
    }

    @Test
    public void logPlaycountCallsOnNextWithTrackUrnOnExpectedResponse() throws Exception {
        final Track track = new Track(1L);
        APIResponse response = mock(APIResponse.class);

        when(rxHttpClient.fetchResponse(argThat(isMobileApiRequestTo("POST", "/tracks/soundcloud%3Asounds%3A1/plays")
                .withQueryParam("client_id", "12345")))).thenReturn(Observable.just(response));
        when(httpProperties.getClientId()).thenReturn("12345");
        when(response.getStatusCode()).thenReturn(302);

        playbackOperations.logPlay(track.getUrn()).subscribe(observer);
        verify(observer).onNext(track.getUrn());

    }


    private void checkSetNewPlayQueueArgs(int startPosition, PlaySessionSource playSessionSource, Long... ids){
        verify(playQueueManager).setNewPlayQueue(
                eq(PlayQueue.fromIdList(Lists.newArrayList(ids), startPosition, playSessionSource)),
                eq(playSessionSource));
    }

    protected void checkLastStartedServiceForPlayCurrentAction() {
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toEqual(PlaybackService.Actions.PLAY_CURRENT);
    }
}
