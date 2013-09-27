package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.tracking.eventlogger.PlaySourceTrackingInfo;
import com.tobedevoured.modelcitizen.CreateModelException;
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

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlaybackReceiverTest {

    PlaybackReceiver playbackReceiver;

    private @Mock CloudPlaybackService playbackService;
    private @Mock AssociationManager associationManager;
    private @Mock PlayQueueManager playQueueManager;
    private @Mock AudioManager audioManager;
    private @Mock PlayerAppWidgetProvider playerAppWidgetProvider;
    private @Mock AccountOperations accountOperations;

    private PlaySourceTrackingInfo trackingInfo;

    @Before
    public void setup() {
        SoundCloudApplication.MODEL_MANAGER.clear();
        playbackReceiver = new PlaybackReceiver(playbackService, associationManager, playQueueManager, audioManager, accountOperations);
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        when(playbackService.getAppWidgetProvider()).thenReturn(playerAppWidgetProvider);
        trackingInfo = new PlaySourceTrackingInfo("origin-url", "exploreTag");
    }

    @Test
    public void nextActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(CloudPlaybackService.Actions.NEXT_ACTION));
        verify(playbackService).next();
    }

    @Test
    public void prevActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(CloudPlaybackService.Actions.PREVIOUS_ACTION));
        verify(playbackService).prev();
    }

    @Test
    public void togglePlaybackActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(CloudPlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
        verify(playbackService).togglePlayback();
    }

    @Test
    public void pauseActionShouldCallNextOnService() {
        playbackReceiver.onReceive(Robolectric.application, new Intent(CloudPlaybackService.Actions.PAUSE_ACTION));
        verify(playbackService).pause();
    }

    @Test
    public void loadInfoShouldCallLoadErrorWhenIdHasNoLocalTrack() {
        final Intent intent = new Intent(CloudPlaybackService.Actions.LOAD_TRACK_INFO);
        intent.putExtra(Track.EXTRA_ID, 100L);
        final FetchModelTask.Listener listener = Mockito.mock(FetchModelTask.Listener.class);
        when(playbackService.getInfoListener()).thenReturn(listener);

        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(listener).onError(100L);
    }

    @Test
    public void updateAppWidgetProviderActionShouldCallUpdateOnAppWidgetProviderWithPlaystateChangedAction(){
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.UPDATE_WIDGET_ACTION);
        final int[] ids = {1, 2, 3};
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        playbackReceiver.onReceive(Robolectric.application, intent);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(playerAppWidgetProvider).performUpdate(any(Context.class), eq(ids), intentCaptor.capture());
        expect(intentCaptor.getValue().getAction()).toBe(CloudPlaybackService.Broadcasts.PLAYSTATE_CHANGED);
    }

    @Test
    public void shouldCacheAndLoadTrackOnQueueViaParcelable() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.track, track);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, trackingInfo);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).loadTrack(eq(track), eq(true), eq(trackingInfo));
        expect(SoundCloudApplication.MODEL_MANAGER.getCachedTrack(track.getId())).toEqual(track);
    }

    @Test
    public void shouldCacheLoadTrackAndFetchRelatedOnQueueViaParcelable() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.track, track);
        intent.putExtra(CloudPlaybackService.PlayExtras.fetchRelated, true);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, trackingInfo);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).loadTrack(eq(track), eq(true), eq(trackingInfo));
        verify(playQueueManager).fetchRelatedTracks(eq(track));
        expect(SoundCloudApplication.MODEL_MANAGER.getCachedTrack(track.getId())).toEqual(track);
    }

    @Test
    public void shouldLoadTrackOnQueueViaTrackId() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackId, track.getId());
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, trackingInfo);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).loadTrack(eq(track), eq(true), eq(trackingInfo));
    }

    @Test
    public void shouldLoadUriOnQueue() throws CreateModelException {
        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, 2);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, trackingInfo);
        intent.setData(Content.ME_LIKES.uri);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).loadUri(eq(Content.ME_LIKES.uri), eq(2), (List<? extends PlayableHolder>) eq(null), eq(2), eq(trackingInfo));
    }

    @Test
    public void shouldLoadUriOnQueueWithInitalPlaylist() throws CreateModelException {
        CloudPlaybackService.playlistXfer = Lists.newArrayList(TestHelper.getModelFactory().createModel(Track.class));

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, 2);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, trackingInfo);
        intent.setData(Content.ME_LIKES.uri);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).loadUri(eq(Content.ME_LIKES.uri), eq(2), eq(CloudPlaybackService.playlistXfer), eq(2), eq(trackingInfo));
    }

    @Test
    public void shouldSetPlayQueueWithPositionOnPlayIntentWithNoTrackOrUri() throws CreateModelException {
        final ArrayList<Track> transferList = Lists.newArrayList(TestHelper.getModelFactory().createModel(Track.class));
        CloudPlaybackService.playlistXfer = transferList;

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.playFromXferList, true);
        intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, 2);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, trackingInfo);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).setPlayQueue(transferList, 2, trackingInfo);
    }

    @Test
    public void shouldSetPlayQueueToLoadUriIfUriEvenWithInitalTrack() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        CloudPlaybackService.playlistXfer = null;

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.track, track);
        intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, 2);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, trackingInfo);
        intent.setData(Content.ME_LIKES.uri);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).loadUri(eq(Content.ME_LIKES.uri), eq(2), (List<? extends PlayableHolder>) eq(null), eq(2), eq(trackingInfo));
    }

    @Test
    public void shouldSetPlayQueueToTransferListEvenWithInitalTrack() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        final ArrayList<Track> transferList = Lists.newArrayList(TestHelper.getModelFactory().createModel(Track.class));
        CloudPlaybackService.playlistXfer = transferList;

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.track, track);
        intent.putExtra(CloudPlaybackService.PlayExtras.playFromXferList, true);
        intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, 2);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, trackingInfo);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(playQueueManager).setPlayQueue(transferList, 2, trackingInfo);
    }

    @Test
    public void shouldAddLikeForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.ADD_LIKE_ACTION);
        intent.setData(track.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setLike(isA(Track.class), eq(true));
    }

    @Test
    public void shouldRemoveLikeForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.REMOVE_LIKE_ACTION);
        intent.setData(track.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setLike(isA(Track.class), eq(false));
    }

    @Test
    public void shouldAddLikeForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.Actions.ADD_LIKE_ACTION);
        intent.setData(playlist.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setLike(isA(Playlist.class), eq(true));
    }

    @Test
    public void shouldRemoveLikeForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.Actions.REMOVE_LIKE_ACTION);
        intent.setData(playlist.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setLike(isA(Playlist.class), eq(false));
    }

    @Test
    public void shouldAddRepostForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.ADD_REPOST_ACTION);
        intent.setData(track.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setRepost(isA(Track.class), eq(true));
    }

    @Test
    public void shouldRemoveRepostForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.REMOVE_REPOST_ACTION);
        intent.setData(track.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setRepost(isA(Track.class), eq(false));
    }

    @Test
    public void shouldAddRepostForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.Actions.ADD_REPOST_ACTION);
        intent.setData(playlist.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setRepost(isA(Playlist.class), eq(true));
    }

    @Test
    public void shouldRemoveRepostForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.Actions.REMOVE_REPOST_ACTION);
        intent.setData(playlist.toUri());

        playbackReceiver.onReceive(Robolectric.application, intent);

        verify(associationManager).setRepost(isA(Playlist.class), eq(false));
    }

    @Test
    public void shouldCallResetAllOnServiceAndClearPlayqueueOnResetAllAction(){
        Intent intent = new Intent(CloudPlaybackService.Actions.RESET_ALL);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).resetAll();
        verify(playQueueManager).clear();
    }

    @Test
    public void shouldCallSaveProgressAndStopOnStopActionIfPlaying(){
        when(playbackService.getState()).thenReturn(State.PLAYING);
        Intent intent = new Intent(CloudPlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).saveProgressAndStop();
    }

    @Test
    public void shouldCallStopOnStopActionIfNotPlaying(){
        when(playbackService.getState()).thenReturn(State.PAUSED);
        Intent intent = new Intent(CloudPlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).stop();
    }

    @Test
    public void shouldNotCallSaveProgressAndStopOnStopActionIfNotPlaying(){
        when(playbackService.getState()).thenReturn(State.STOPPED);
        Intent intent = new Intent(CloudPlaybackService.Actions.STOP_ACTION);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService, never()).saveProgressAndStop();
    }

    @Test
    public void shouldCallResetAllWithNoAccount(){
        Intent intent = new Intent(CloudPlaybackService.Actions.RESET_ALL);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService).resetAll();
        verify(playQueueManager).clear();
        verifyZeroInteractions(accountOperations);
    }

    @Test
    public void shouldOpenCurrentIfPlayQueueChangedFromEmptyPlaylist(){
        when(playbackService.getState()).thenReturn(State.EMPTY_PLAYLIST);
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verify(playbackService, never()).saveProgressAndStop();
    }

    @Test
    public void shouldNotInteractWithThePlayBackServiceIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(playbackService);
    }

    @Test
    public void shouldNotInteractWithTheAssociationManagerIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(associationManager);
    }

    @Test
    public void shouldNotInteractWithThePlayqueueManagerIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(playQueueManager);
    }

    @Test
    public void shouldNotInteractWithTheAudioManagerIfNoAccountExists(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED);
        playbackReceiver.onReceive(Robolectric.application, intent);
        verifyZeroInteractions(audioManager);
    }
}
