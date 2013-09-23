package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;

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
}
