package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayUtilsTest {

    PlayUtils playUtils;
    PlayInfo playInfo;
    Track track;

    @Mock
    Context context;
    @Mock
    ScModelManager modelManager;

    @Before
    public void setUp() throws Exception {
        playUtils = new PlayUtils(context, modelManager);
        track = TestHelper.getModelFactory().createModel(Track.class);
        playInfo = new PlayInfo(track);
    }

    @Test
    public void getPlayIntentShouldUsePlayAction() throws Exception {
        expect(playUtils.getPlayIntent(playInfo).getAction()).toBe(Actions.PLAY);
    }

    @Test
    public void getPlayIntentShouldNotAddAnyExtrasIfNotChangingTracks() throws Exception {
        expect(playUtils.getPlayIntent(playInfo, false).getExtras()).toBeNull();
    }

    @Test
    public void getPlayIntentShouldAddInitialTrackAsParcelableFromInfo() throws Exception {
        final Intent playIntent = playUtils.getPlayIntent(playInfo, true);
        expect(playIntent.getParcelableExtra(CloudPlaybackService.PlayExtras.track)).toEqual(track);
    }

    @Test
    public void getPlayIntentShouldAddFetchRelatedFromInfo() throws Exception {
        playInfo.fetchRelated = true;
        final Intent playIntent = playUtils.getPlayIntent(playInfo, true);
        expect(playIntent.getBooleanExtra(CloudPlaybackService.PlayExtras.fetchRelated, false)).toBeTrue();
    }

    @Test
    public void getPlayIntentShouldAddTrackingFromInfo() throws Exception {
        playInfo.trackingInfo = new PlaySourceInfo("source-context", "explore-tag");
        final Intent playIntent = playUtils.getPlayIntent(playInfo, true);
        expect(playIntent.getSerializableExtra(CloudPlaybackService.PlayExtras.trackingInfo)).toEqual(playInfo.trackingInfo);
    }

    @Test
    public void getPlayIntentShouldConfigureIntentViaUri() throws Exception {
        playInfo.uri = Content.ME_LIKES.uri;
        playInfo.position = 4;

        final Intent playIntent = playUtils.getPlayIntent(playInfo, true);
        expect(playIntent.getData()).toEqual(Content.ME_LIKES.uri);
        expect(playIntent.getIntExtra(CloudPlaybackService.PlayExtras.playPosition, 0)).toEqual(4);
        expect(playIntent.getLongExtra(CloudPlaybackService.PlayExtras.trackId, -1L)).toEqual(track.getId());
        expect(CloudPlaybackService.playlistXfer).toBe(playInfo.playables);
        verify(modelManager).cache(track);
    }

    @Test
    public void getPlayIntentShouldConfigureIntentViaPlayablesList() throws Exception {
        playInfo.playables = Lists.newArrayList(track, TestHelper.getModelFactory().createModel(Track.class), TestHelper.getModelFactory().createModel(Track.class));
        playInfo.position = 2;

        final Intent playIntent = playUtils.getPlayIntent(playInfo, true);
        expect(playIntent.getBooleanExtra(CloudPlaybackService.PlayExtras.playFromXferList, false)).toBeTrue();
        expect(CloudPlaybackService.playlistXfer).toBe(playInfo.playables);
    }

    @Test
    public void getPlayIntentShouldNotAddPlayablesListIfPlaying1ItemList() throws Exception {
        playInfo.playables = Lists.newArrayList(track);
        final Intent playIntent = playUtils.getPlayIntent(playInfo, true);
        expect(CloudPlaybackService.playlistXfer).toBe(playInfo.playables);
        expect(playIntent.getExtras().containsKey(CloudPlaybackService.PlayExtras.playPosition)).toBeFalse();
        expect(playIntent.getExtras().containsKey(CloudPlaybackService.PlayExtras.playFromXferList)).toBeFalse();
    }

    @Test(expected=AssertionError.class)
    public void playFromAdapterShouldThrowAssertionErrorWhenPositionGreaterThanSize() throws Exception {
        List<Playable> playables = Lists.<Playable>newArrayList(track);
        playUtils.playFromAdapter(playables, 1, Content.ME_LIKES.uri);
    }

    @Test
    public void playFromAdapterShouldSetInitialTrackFromPosition() throws Exception {
        final Track track2 = TestHelper.getModelFactory().createModel(Track.class);
        List<Playable> playables = Lists.<Playable>newArrayList(track, track2);
        playUtils.playFromAdapter(playables, 1, Content.ME_LIKES.uri);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(captor.capture());
        expect(captor.getValue().getParcelableExtra(CloudPlaybackService.PlayExtras.track)).toEqual(track2);
    }

    @Test
    public void playFromAdapterShouldSetPlayablesFromItemList() throws Exception {
        List<Track> playables = Lists.newArrayList(track, TestHelper.getModelFactory().createModel(Track.class));
        playUtils.playFromAdapter(playables, 1, Content.ME_LIKES.uri);
        expect(CloudPlaybackService.playlistXfer).toEqual(playables);
    }

    @Test
    public void playFromAdapterShouldSetPlayablesFromItemListWithNonTracksRemoved() throws Exception {
        final Track track2 = TestHelper.getModelFactory().createModel(Track.class);
        final Playlist playlist = Mockito.mock(Playlist.class);
        List<Playable> playables = Lists.newArrayList(track, playlist, track2);

        playUtils.playFromAdapter(playables, 2, Content.ME_LIKES.uri);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(captor.capture());
        expect(captor.getValue().getIntExtra(CloudPlaybackService.PlayExtras.playPosition, -1)).toEqual(1);
        expect(CloudPlaybackService.playlistXfer).toContainExactly(track, track2);
    }

    @Test
    public void playFromAdapterShouldStartPlaylistActivity() throws Exception {
        final Playlist playlist = Mockito.mock(Playlist.class);
        final Uri playlistUri = Uri.parse("playlist/uri");
        when(playlist.toUri()).thenReturn(playlistUri);
        when(playlist.getPlayable()).thenReturn(playlist);

        List<Playable> playables = Lists.newArrayList(track, playlist);
        playUtils.playFromAdapter(playables, 1, Content.ME_SOUND_STREAM.uri);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startActivity(captor.capture());
        final Intent intent = captor.getValue();

        expect(intent.getData()).toEqual(playlistUri);
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        verify(modelManager).cache(playlist);

    }
}
