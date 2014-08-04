package com.soundcloud.android.deeplinks;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class ResolveActivityTest {
    private ResolveActivity activity;
    @Mock private PlaybackOperations playbackOperations;

    @Before
    public void setUp() throws Exception {
        activity = new ResolveActivity(playbackOperations);
    }

    @Test
    public void shouldPlayTrack() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        activity.onSuccess(track);

        verify(playbackOperations).startPlaybackWithRecommendations(track, Screen.DEEPLINK);
    }

    @Test
    public void shouldGotoPlaylistDetails() throws CreateModelException {
        PublicApiPlaylist playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        activity.onSuccess(playlist);

        Intent expected = new Intent(Actions.PLAYLIST);
        Screen.DEEPLINK.addToIntent(expected);
        expected.putExtra(PublicApiPlaylist.EXTRA_URN, playlist.getUrn());
        expect(shadowOf(activity).getNextStartedActivity()).toEqual(expected);
    }

    @Test
    public void shouldShowTheStreamWithAnExpandedPlayer() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        activity.onSuccess(track);

        Intent expected = new Intent(Actions.STREAM);
        expected.putExtra("EXPAND_PLAYER", true);
        expect(shadowOf(activity).getNextStartedActivity()).toEqual(expected);
    }
}