package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.PlaybackService.PlayExtras;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.soundcloud.android.Actions;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
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
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
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


    @Before
    public void setUp() throws Exception {
        playbackOperations = new PlaybackOperations(modelManager, trackStorage, playQueueManager, accountOperations,
                httpProperties, featureFlags);
        track = TestHelper.getModelFactory().createModel(Track.class);
    }

    @Test
    public void playTrackShouldOpenPlayerActivityWithInitialTrackId() {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(false);
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(track.getId());
    }

    @Test
    public void playTrackShouldNotOpenPlayerActivityIfVisualPlayerEnabled() {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
     public void playTrackShouldStartPlaybackServiceWithPlayQueueFromInitialTrack() {
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
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
    public void playExploreTrackSendsServiceIntentWithPlayQueueAndOriginSet() {
        playbackOperations.playExploreTrack(Robolectric.application, track, EXPLORE_VERSION, ORIGIN_SCREEN.get());

        final PlaySessionSource expected = new PlaySessionSource(ORIGIN_SCREEN.get());
        expected.setExploreVersion(EXPLORE_VERSION);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 0, expected, track.getId());
    }

    @Test
    public void playExploreTrackSetsLoadRecommendedOnIntent() {
        playbackOperations.playExploreTrack(Robolectric.application, track, EXPLORE_VERSION, ORIGIN_SCREEN.get());

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        final Intent nextStartedService = application.getNextStartedService();
        expect(nextStartedService.getBooleanExtra(PlayExtras.LOAD_RECOMMENDED, false)).toBeTrue();
    }

    @Test
    public void playFromPlaylistShouldOpenPlayerActivityWithInitialTrackId() {
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
    public void playFromPlaylistShouldNotOpenPlayerActivityWithVisualPlayerTurnedOn() {
        final Observable<List<Long>> trackIdList = Observable.<List<Long>>just(Lists.newArrayList(track.getId()));
        when(trackStorage.getTrackIdsForUriAsync(any(Uri.class))).thenReturn(trackIdList);
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);
        playbackOperations.playPlaylistFromPosition(Robolectric.application, new Playlist(3L), 0, track, Screen.YOUR_LIKES);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
    public void playFromPlaylistShouldStartPlaybackServiceWithPlayQueueFromPlaylist() throws CreateModelException {
        List<Track> tracks = TestHelper.createTracks(3);
        Playlist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        final ArrayList<Long> trackIds = Lists.newArrayList(tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
        when(trackStorage.getTrackIdsForUriAsync(playlist.toUri())).thenReturn(Observable.<List<Long>>just(trackIds));

        playbackOperations.playPlaylistFromPosition(Robolectric.application, playlist, 1, tracks.get(1), ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist);
        checkStartIntent(application.getNextStartedService(), 1, playSessionSource,
                tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
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
        when(playQueueManager.getCurrentTrackId()).thenReturn(1L);
        playbackOperations.togglePlayback(Robolectric.application);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toBe(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION);
    }

    @Test
    public void shouldStartPlaybackServiceWithPlaylistTrackIds() throws CreateModelException {
        List<Track> tracks = TestHelper.createTracks(3);
        Playlist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        playbackOperations.playPlaylist(Robolectric.application, playlist, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        final PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_SCREEN.get());
        playSessionSource.setPlaylist(playlist);
        checkStartIntent(application.getNextStartedService(), 0, playSessionSource,
                tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playFromIdsShuffledShouldStartPlayerWithGivenTrackIdList() {
        final ArrayList<Long> idsOrig = Lists.newArrayList(1L, 2L, 3L);
        playbackOperations.playFromIdListShuffled(Robolectric.application, idsOrig, Screen.YOUR_LIKES);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);

        Intent startintent = application.getNextStartedService();
        expect(startintent).not.toBeNull();
        expect(startintent.getAction()).toBe(PlaybackService.Actions.PLAY_ACTION);
        expect(startintent.getIntExtra(PlayExtras.START_POSITION, -1)).toBe(0);
        expect(startintent.getParcelableExtra(PlayExtras.PLAY_SESSION_SOURCE)).toEqual(new PlaySessionSource(Screen.YOUR_LIKES));

        final List<Long> trackIdList = Longs.asList(startintent.getLongArrayExtra(PlayExtras.TRACK_ID_LIST));
        expect(Sets.newHashSet(trackIdList)).toContainExactly(1L, 2L, 3L);
        expect(trackIdList).not.toBe(idsOrig);
    }

    @Test
    public void playFromAdapterShouldRemoveDuplicates() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L), new Track(3L), new Track(2L), new Track(1L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 4, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 2,  new PlaySessionSource(ORIGIN_SCREEN.get()), 2L, 3L, 1L);

    }

    private void checkStartIntent(Intent startintent, int startPosition, PlaySessionSource playSessionSource, Long... ids){
        expect(startintent).not.toBeNull();
        expect(startintent.getAction()).toBe(PlaybackService.Actions.PLAY_ACTION);
        expect(startintent.getIntExtra(PlayExtras.START_POSITION, -1)).toBe(startPosition);
        expect(startintent.getParcelableExtra(PlayExtras.PLAY_SESSION_SOURCE)).toEqual(playSessionSource);
        final List<Long> trackIdList = Longs.asList(startintent.getLongArrayExtra(PlayExtras.TRACK_ID_LIST));
        expect(trackIdList).toContainExactly(ids);
    }

    @Test
    public void playFromAdapterShouldOpenPlayerActivityWithInitialTrackFromPosition() throws Exception {
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
    public void playFromAdapterShouldNotOpenPlayerActivityIfVisualPlayerActive() throws Exception {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);

        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, Screen.SIDE_MENU_STREAM); // clicked 2nd track

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedActivity()).toBeNull();
    }

    @Test
    public void playFromAdapterShouldStartPlaybackServiceWithListOfTracks() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterShouldIgnoreItemsThatAreNotTracks() throws Exception {
        List<Playable> playables = Lists.newArrayList(new Track(1L), new Playlist(), new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 1, new PlaySessionSource(ORIGIN_SCREEN.get()), 1L, 2L);
    }

    @Test
    public void playFromAdapterWithUriShouldAdjustPlayPositionWithUpdatedContent() throws IOException {
        final List<Playable> playables = Lists.newArrayList(new Track(1L), new Playlist(), new Track(2L));
        final ArrayList<Long> value = Lists.newArrayList(5L, 1L, 2L);

        when(trackStorage.getTrackIdsForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.<List<Long>>just(value));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, Content.ME_LIKES.uri, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 2,  new PlaySessionSource(ORIGIN_SCREEN.get()), 5L, 1L, 2L);
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
    public void startPlaybackWithTrackCachesTrackAndSendsConfiguredPlaybackIntent() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        playbackOperations.startPlaybackWithRecommendations(Robolectric.application, track, ORIGIN_SCREEN);

        verify(modelManager).cache(track);
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());

    }

    @Test
    public void startPlaybackWithRecommendationsByTrackSendsConfiguredPlaybackIntent() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        playbackOperations.startPlaybackWithRecommendations(Robolectric.application, track, ORIGIN_SCREEN);
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        final Intent nextStartedService = application.getNextStartedService();
        checkStartIntent(nextStartedService, 0, new PlaySessionSource(ORIGIN_SCREEN.get()), track.getId());
    }

    @Test
    public void startPlaybackWithRecommendationsByTrackCachesTrackParameter() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        playbackOperations.startPlaybackWithRecommendations(Robolectric.application, track, ORIGIN_SCREEN);
        verify(modelManager).cache(track);
    }

    @Test
    public void startPlaybackWithRecommendationsByTrackSendsIntentWithLoadRecommendedExtraAsTrue() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        playbackOperations.startPlaybackWithRecommendations(Robolectric.application, track, ORIGIN_SCREEN);
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        final Intent nextStartedService = application.getNextStartedService();
        expect(nextStartedService.getBooleanExtra(PlayExtras.LOAD_RECOMMENDED, false)).toBeTrue();
    }

    @Test
    public void startPlaybackWithRecommendationsByIdSendsConfiguredPlaybackIntent() throws Exception {
        playbackOperations.startPlaybackWithRecommendations(Robolectric.application, 123L, ORIGIN_SCREEN);
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        final Intent nextStartedService = application.getNextStartedService();
        checkStartIntent(nextStartedService, 0, new PlaySessionSource(ORIGIN_SCREEN.get()), 123L);
    }

    @Test
    public void startPlaybackWithRecommendationsByIdSendsIntentWithLoadRecommendedExtraAsTrue() throws Exception {
        playbackOperations.startPlaybackWithRecommendations(Robolectric.application, 123L, ORIGIN_SCREEN);
        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        final Intent nextStartedService = application.getNextStartedService();
        expect(nextStartedService.getBooleanExtra(PlayExtras.LOAD_RECOMMENDED, false)).toBeTrue();
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

}
