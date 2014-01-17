package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.primitives.Longs;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.AssociationManager;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.tasks.FetchModelTask;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlaybackReceiverTest {

    private PlaybackReceiver playbackReceiver;

    @Mock
    private PlaybackService playbackService;
    @Mock
    private AssociationManager associationManager;
    @Mock
    private PlayQueueView playQueue;
    @Mock
    private AudioManager audioManager;
    @Mock
    private PlayerAppWidgetProvider playerAppWidgetProvider;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private PlaySessionSource playSessionSource;

    @Before
    public void setup() {
        SoundCloudApplication.sModelManager.clear();
        playbackReceiver = new PlaybackReceiver(playbackService, associationManager, audioManager, accountOperations, playQueueManager);
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        when(playbackService.getAppWidgetProvider()).thenReturn(playerAppWidgetProvider);
        when(playbackService.getPlayQueueOriginScreen()).thenReturn("screen_tag");
    }

    @Test
    public void nextActionShouldCallNextAndOpenCurrentIfNextSuccessful() {
        when(playbackService.next()).thenReturn(true);
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.NEXT_ACTION));
        verify(playbackService).openCurrent();
    }

    @Test
    public void nextActionShouldCallNextAndNotOpenCurrentIfNextNotSuccessful() {
        when(playbackService.next()).thenReturn(false);
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.NEXT_ACTION));
        verify(playbackService).next();
        verify(playbackService, never()).openCurrent();
    }

    @Test
    public void prevActionShouldCallPrevAndOpenCurrentIfPrevSuccessful() {
        when(playbackService.prev()).thenReturn(true);
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.PREVIOUS_ACTION));
        verify(playbackService).openCurrent();
    }

    @Test
    public void prevActionShouldCallPrevAndNotOpenCurrentIfPrevNotSuccessful() {
        when(playbackService.prev()).thenReturn(false);
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.PREVIOUS_ACTION));
        verify(playbackService).prev();
        verify(playbackService, never()).openCurrent();
    }

    @Test
    public void togglePlaybackActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
        verify(playbackService).togglePlayback();
    }

    @Test
    public void pauseActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(PlaybackService.Actions.PAUSE_ACTION));
        verify(playbackService).pause();
    }

    @Test
    public void loadInfoShouldCallLoadErrorWhenIdHasNoLocalTrack() {
        final Intent intent = new Intent(PlaybackService.Actions.LOAD_TRACK_INFO);
        intent.putExtra(Track.EXTRA_ID, 100L);
        final FetchModelTask.Listener listener = Mockito.mock(FetchModelTask.Listener.class);
        when(playbackService.getInfoListener()).thenReturn(listener);

        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(listener).onError(100L);
    }

    @Test
    public void updateAppWidgetProviderActionShouldCallUpdateOnAppWidgetProviderWithPlaystateChangedAction(){
        Intent intent = new Intent(PlaybackService.Broadcasts.UPDATE_WIDGET_ACTION);
        final int[] ids = {1, 2, 3};
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        playbackReceiver.onReceive(Robolectric.application, intent);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(playerAppWidgetProvider).performUpdate(any(Context.class), eq(ids), intentCaptor.capture());
        expect(intentCaptor.getValue().getAction()).toBe(PlaybackService.Broadcasts.PLAYSTATE_CHANGED);
    }

    @Test
    public void shouldCreateAndSetPlayQueueOnPlayQueueManager() {
        final long[] idListArray = new long[]{1L, 2L, 3L};
        final List<Long> idList = Longs.asList(idListArray);

        Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.TRACK_ID_LIST, idListArray);
        intent.putExtra(PlaybackService.PlayExtras.PLAY_SESSION_SOURCE, playSessionSource);
        intent.putExtra(PlaybackService.PlayExtras.START_POSITION, 2);

        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playQueueManager).setNewPlayQueue(eq(PlayQueue.fromIdList(idList, 2, playSessionSource)), eq(playSessionSource));
    }

    @Test
    public void sendingNewPlayQueueShouldOpenCurrentTrackInPlaybackService() {
        Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.TRACK_ID_LIST, new long[]{1L, 2L, 3L});
        intent.putExtra(PlaybackService.PlayExtras.PLAY_SESSION_SOURCE, playSessionSource);
        intent.putExtra(PlaybackService.PlayExtras.START_POSITION, 2);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playbackService).openCurrent();
    }

    @Test
    public void sendingNewPlayQueueShouldOptionallyFetchRelatedTracks() {
        when(playSessionSource.originatedInExplore()).thenReturn(true);
        Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.TRACK_ID_LIST, new long[]{1L});
        intent.putExtra(PlaybackService.PlayExtras.PLAY_SESSION_SOURCE, playSessionSource);
        intent.putExtra(PlaybackService.PlayExtras.START_POSITION, 0);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).fetchRelatedTracks(1L);
    }

    @Test
    public void shouldAddLikeForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.sModelManager.cache(track);

        Intent intent = new Intent(PlaybackService.Actions.ADD_LIKE_ACTION);
        intent.setData(track.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setLike(isA(Track.class), eq(true));
    }

    @Test
    public void shouldRemoveLikeForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.sModelManager.cache(track);

        Intent intent = new Intent(PlaybackService.Actions.REMOVE_LIKE_ACTION);
        intent.setData(track.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setLike(isA(Track.class), eq(false));
    }

    @Test
    public void shouldAddLikeForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/sync/playlist.json");
        SoundCloudApplication.sModelManager.cache(playlist);

        Intent intent = new Intent(PlaybackService.Actions.ADD_LIKE_ACTION);
        intent.setData(playlist.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setLike(isA(Playlist.class), eq(true));
    }

    @Test
    public void shouldRemoveLikeForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/sync/playlist.json");
        SoundCloudApplication.sModelManager.cache(playlist);

        Intent intent = new Intent(PlaybackService.Actions.REMOVE_LIKE_ACTION);
        intent.setData(playlist.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setLike(isA(Playlist.class), eq(false));
    }

    @Test
    public void shouldAddRepostForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.sModelManager.cache(track);

        Intent intent = new Intent(PlaybackService.Actions.ADD_REPOST_ACTION);
        intent.setData(track.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setRepost(isA(Track.class), eq(true), eq("screen_tag"));
    }

    @Test
    public void shouldRemoveRepostForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.sModelManager.cache(track);

        Intent intent = new Intent(PlaybackService.Actions.REMOVE_REPOST_ACTION);
        intent.setData(track.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setRepost(isA(Track.class), eq(false), eq("screen_tag"));
    }

    @Test
    public void shouldAddRepostForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/sync/playlist.json");
        SoundCloudApplication.sModelManager.cache(playlist);

        Intent intent = new Intent(PlaybackService.Actions.ADD_REPOST_ACTION);
        intent.setData(playlist.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setRepost(isA(Playlist.class), eq(true), eq("screen_tag"));
    }

    @Test
    public void shouldRemoveRepostForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/sync/playlist.json");
        SoundCloudApplication.sModelManager.cache(playlist);

        Intent intent = new Intent(PlaybackService.Actions.REMOVE_REPOST_ACTION);
        intent.setData(playlist.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setRepost(isA(Playlist.class), eq(false), eq("screen_tag"));
    }

    @Test
    public void shouldCallResetAllOnServiceAndClearPlayqueueOnResetAllAction(){
        Intent intent = new Intent(PlaybackService.Actions.RESET_ALL);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).resetAll();
        //verify(playQueue).clear();
    }

    @Test
    public void shouldCallSaveProgressAndStopOnStopActionIfPlaying(){
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.PLAYING);
        Intent intent = new Intent(PlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).saveProgressAndStop();
    }

    @Test
    public void shouldCallStopOnStopActionIfNotPlaying(){
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.PAUSED);
        Intent intent = new Intent(PlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).stop();
    }

    @Test
    public void shouldNotCallSaveProgressAndStopOnStopActionIfNotPlaying(){
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.STOPPED);
        Intent intent = new Intent(PlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService, never()).saveProgressAndStop();
    }

    @Test
    public void shouldCallResetAllWithNoAccount(){
        Intent intent = new Intent(PlaybackService.Actions.RESET_ALL);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).resetAll();
        //verify(playQueue).clear();
        verifyZeroInteractions(accountOperations);
    }

    @Test
    public void shouldOpenCurrentIfPlayQueueChangedFromEmptyPlaylist(){
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.WAITING_FOR_PLAYLIST);
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService, never()).saveProgressAndStop();
    }

    @Test
    public void shouldNotInteractWithThePlayBackServiceIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(playbackService);
    }

    @Test
    public void shouldNotInteractWithTheAssociationManagerIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(associationManager);
    }

    @Test
    public void shouldNotInteractWithThePlayqueueManagerIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void shouldNotInteractWithTheAudioManagerIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(audioManager);
    }
}
