package com.soundcloud.android.ads;


import com.soundcloud.android.playback.VideoAdPlaybackItem;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class InlayAdPlayerTest extends AndroidUnitTest {

    private static final VideoAdPlaybackItem VIDEO_ITEM = VideoAdPlaybackItem.create(AdFixtures.getVideoAd(1L), 0L, 0L);

    @Mock MediaPlayerAdapter adapter;

    private InlayAdPlayer player;

    @Before
    public void setUp() {
        player = new InlayAdPlayer(adapter);
    }

    @Test
    public void playForwardsPlayToMediaPlayerAdapter() {
        player.play(VIDEO_ITEM);

        verify(adapter).stopForTrackTransition();
        verify(adapter).play(VIDEO_ITEM);
    }

    @Test
    public void playCallsResumeOnMediaPlayerAdapterIfSameItemAndIsPaused() {
        player.onPlaystateChanged(TestPlayerTransitions.idle(VIDEO_ITEM.getUrn()));

        player.play(VIDEO_ITEM);

        verify(adapter).resume(VIDEO_ITEM);
    }

    @Test
    public void playCallsPlayOnMediaPlayerAdapterIfDifferentItemPlaying() {
        player.onPlaystateChanged(TestPlayerTransitions.playing());

        player.play(VIDEO_ITEM);

        verify(adapter).stopForTrackTransition();
        verify(adapter).play(VIDEO_ITEM);
    }

    @Test
    public void pauseForwardsPauseCallToMediaPlayerAdapter() {
        player.pause();

        verify(adapter).pause();
    }

    @Test
    public void isPlayingReturnsTrueIfLastStateWasPlaying() {
        player.play(VIDEO_ITEM);
        player.onPlaystateChanged(TestPlayerTransitions.playing());

        assertThat(player.isPlaying()).isTrue();
    }

    @Test
    public void isPlayingReturnFalseIfLastStateWasIdle() {
        player.play(VIDEO_ITEM);
        player.onPlaystateChanged(TestPlayerTransitions.idle());

        assertThat(player.isPlaying()).isFalse();
    }

    @Test
    public void isPlayingReturnFalseIfLastStateWasBuffering() {
        player.play(VIDEO_ITEM);
        player.onPlaystateChanged(TestPlayerTransitions.buffering());

        assertThat(player.isPlaying()).isFalse();
    }
}
