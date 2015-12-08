package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class ShowPlayerSubscriberTest extends AndroidUnitTest {

    @Mock private PlaybackToastHelper playbackToastHelper;

    private ShowPlayerSubscriber subscriber;
    private TestEventBus eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new ShowPlayerSubscriber(eventBus, playbackToastHelper);
    }

    @Test
    public void showsPlayerAsCollapsedOnSuccessfulPlaybackResult() {
        subscriber.onNext(PlaybackResult.success());

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isCollapse()).isTrue();
    }

    @Test
    public void showsToastOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS);

        subscriber.onNext(errorResult);

        verify(playbackToastHelper).showToastOnPlaybackError(errorResult.getErrorReason());
    }
}
