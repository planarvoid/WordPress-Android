package com.soundcloud.android.deeplinks;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
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
    @Mock private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);
        activity = new ResolveActivity(playbackOperations, featureFlags);
    }

    @Test
    public void shouldPlayTrack() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        activity.onSuccess(track);

        verify(playbackOperations).playTrack(activity, track, Screen.DEEPLINK);
    }

    @Test
    public void shouldPlayPlaylist() throws CreateModelException {
        PublicApiPlaylist playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        activity.onSuccess(playlist);

        verify(playbackOperations).playPlaylist(playlist, Screen.DEEPLINK);
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