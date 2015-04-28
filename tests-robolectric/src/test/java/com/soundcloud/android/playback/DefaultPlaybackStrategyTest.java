package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class DefaultPlaybackStrategyTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);

    private DefaultPlaybackStrategy defaultPlaybackStrategy;

    @Mock private PlayQueueManager playQueueManager;

    @Before
    public void setUp() throws Exception {
        defaultPlaybackStrategy = new DefaultPlaybackStrategy(Robolectric.application, playQueueManager);
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
    public void reloadAndPlayCurrentQueueThrowsExceptionCauseItIsntUsedByDefault() {
        defaultPlaybackStrategy.reloadAndPlayCurrentQueue(123L);
    }

    @Test
    public void togglePlaybackSendsTogglePlaybackIntent() throws Exception {
        defaultPlaybackStrategy.togglePlayback();

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toBe(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION);
    }

    @Test
    public void playNewQueueOpensCurrentTrackThroughService() {
        defaultPlaybackStrategy.playNewQueue(Arrays.asList(TRACK1), TRACK1, 0, PlaySessionSource.EMPTY);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        expect(application.getNextStartedService().getAction()).toEqual(PlaybackService.Actions.PLAY_CURRENT);
    }

    @Test
    public void playNewQueueRemovesDuplicates() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.playNewQueue(Lists.newArrayList(TRACK1, TRACK2, TRACK3, TRACK2, TRACK1),
                TRACK1, 0, playSessionSource);

        PlayQueue expectedPlayQueue = PlayQueue.fromTrackUrnList(Lists.newArrayList(TRACK1, TRACK2, TRACK3), playSessionSource);
        verify(playQueueManager).setNewPlayQueue(eq(expectedPlayQueue), eq(0), eq(playSessionSource));
    }

    @Test
    public void playNewQueueShouldFallBackToPositionZeroIfInitialTrackNotFound() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.playNewQueue(Lists.newArrayList(TRACK1, TRACK2),
                TRACK1, 2, playSessionSource);

        PlayQueue expectedPlayQueue = PlayQueue.fromTrackUrnList(Lists.newArrayList(TRACK1, TRACK2), playSessionSource);
        verify(playQueueManager).setNewPlayQueue(eq(expectedPlayQueue), eq(0), eq(playSessionSource));
    }

    @Test
    public void seekSendsSeekIntentWithPosition() throws Exception {
        defaultPlaybackStrategy.seek(123L);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent sentIntent = application.getNextStartedService();
        expect(sentIntent.getAction()).toBe(PlaybackService.Actions.SEEK);
        expect(sentIntent.getLongExtra(PlaybackService.ActionsExtras.SEEK_POSITION, 0L)).toEqual(123L);
    }

}