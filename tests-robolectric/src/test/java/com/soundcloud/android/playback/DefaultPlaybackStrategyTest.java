package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class DefaultPlaybackStrategyTest {

    private DefaultPlaybackStrategy defaultPlaybackStrategy;

    @Before
    public void setUp() throws Exception {
        defaultPlaybackStrategy = new DefaultPlaybackStrategy(Robolectric.application);
    }

    @Test
    public void playCurrentOpensCurrentTrackThroughService() throws Exception {
        defaultPlaybackStrategy.playCurrent();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toEqual(PlaybackService.Actions.PLAY_CURRENT);
    }

    @Test
    public void pausePausesTrackThroughService() throws Exception {
        defaultPlaybackStrategy.pause();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toEqual(PlaybackService.Actions.PAUSE_ACTION);
    }

    @Test
    public void resumePlaysTrackThroughService() throws Exception {
        defaultPlaybackStrategy.resume();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toEqual(PlaybackService.Actions.PLAY_ACTION);
    }

    @Test(expected = IllegalStateException.class)
    public void playCurrentFromPositionThrowsExceptionCauseItIsntUsedByDefault() {
        defaultPlaybackStrategy.playCurrent(123);
    }

    @Test
    public void togglePlaybackSendsTogglePlaybackIntent() throws Exception {
        defaultPlaybackStrategy.togglePlayback();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toBe(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION);
    }

    @Test
    public void seekSendsSeekIntentWithPosition() throws Exception {
        defaultPlaybackStrategy.seek(123);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(123L);
    }

}