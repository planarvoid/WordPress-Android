package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

import java.util.ArrayList;

@RunWith(DefaultTestRunner.class)
public class CloudPlaybackServiceTest {

    private CloudPlaybackService service;
    private @Mock AssociationManager associationManager;
    private @Mock PlayQueueManager playQueueManager;

    @Before
    public void setup() {
        SoundCloudApplication.MODEL_MANAGER.clear();
        service = new CloudPlaybackService();
        service.onCreate();
        service.setAssociationManager(associationManager);
        service.setPlayqueueManager(playQueueManager);
    }

    @Test
    public void shouldCacheAndLoadTrackOnQueueViaParcelable() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.track, track);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);

        service.onStartCommand(intent, 0, 0);

        verify(playQueueManager).loadTrack(eq(track), eq(true));
        expect(SoundCloudApplication.MODEL_MANAGER.getCachedTrack(track.getId())).toEqual(track);
    }

    @Test
    public void shouldLoadTrackOnQueueViaTrackId() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackId, track.getId());
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);

        service.onStartCommand(intent, 0, 0);

        verify(playQueueManager).loadTrack(eq(track), eq(true));
    }

    @Test
    public void shouldLoadUriOnQueue() throws CreateModelException {
        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, 2);
        intent.setData(Content.ME_LIKES.uri);

        service.onStartCommand(intent, 0, 0);

        verify(playQueueManager).loadUri(Content.ME_LIKES.uri, 2, null, 2);
    }

    @Test
    public void shouldLoadUriOnQueueWithInitalPlaylist() throws CreateModelException {
        CloudPlaybackService.playlistXfer = Lists.newArrayList(TestHelper.getModelFactory().createModel(Track.class));

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, 2);
        intent.setData(Content.ME_LIKES.uri);

        service.onStartCommand(intent, 0, 0);

        verify(playQueueManager).loadUri(Content.ME_LIKES.uri, 2, CloudPlaybackService.playlistXfer, 2);
    }

    @Test
    public void shouldSetPlayQueueWithPositionOnPlayIntentWithNoTrackOrUri() throws CreateModelException {
        final ArrayList<Track> transferList = Lists.newArrayList(TestHelper.getModelFactory().createModel(Track.class));
        CloudPlaybackService.playlistXfer = transferList;

        Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.startPlayback, false);
        intent.putExtra(CloudPlaybackService.PlayExtras.playFromXferCache, true);
        intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, 2);

        service.onStartCommand(intent, 0, 0);

        verify(playQueueManager).setPlayQueue(transferList, 2);
    }

    @Test
    public void shouldAddLikeForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.ADD_LIKE_ACTION);
        intent.setData(track.toUri());

        service.onStartCommand(intent, 0, 0);

        verify(associationManager).setLike(isA(Track.class), eq(true));
    }

    @Test
    public void shouldRemoveLikeForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.REMOVE_LIKE_ACTION);
        intent.setData(track.toUri());

        service.onStartCommand(intent, 0, 0);

        verify(associationManager).setLike(isA(Track.class), eq(false));
    }

    @Test
    public void shouldAddLikeForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.Actions.ADD_LIKE_ACTION);
        intent.setData(playlist.toUri());

        service.onStartCommand(intent, 0, 0);

        verify(associationManager).setLike(isA(Playlist.class), eq(true));
    }

    @Test
    public void shouldRemoveLikeForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.Actions.REMOVE_LIKE_ACTION);
        intent.setData(playlist.toUri());

        service.onStartCommand(intent, 0, 0);

        verify(associationManager).setLike(isA(Playlist.class), eq(false));
    }

    @Test
    public void shouldAddRepostForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.ADD_REPOST_ACTION);
        intent.setData(track.toUri());

        service.onStartCommand(intent, 0, 0);

        verify(associationManager).setRepost(isA(Track.class), eq(true));
    }

    @Test
    public void shouldRemoveRepostForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.Actions.REMOVE_REPOST_ACTION);
        intent.setData(track.toUri());

        service.onStartCommand(intent, 0, 0);

        verify(associationManager).setRepost(isA(Track.class), eq(false));
    }

    @Test
    public void shouldAddRepostForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.Actions.ADD_REPOST_ACTION);
        intent.setData(playlist.toUri());

        service.onStartCommand(intent, 0, 0);

        verify(associationManager).setRepost(isA(Playlist.class), eq(true));
    }

    @Test
    public void shouldRemoveRepostForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.Actions.REMOVE_REPOST_ACTION);
        intent.setData(playlist.toUri());

        service.onStartCommand(intent, 0, 0);

        verify(associationManager).setRepost(isA(Playlist.class), eq(false));
    }
}
