package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ShowPlayerSubscriberTest extends AndroidUnitTest {

    @Mock private PlaybackFeedbackHelper playbackFeedbackHelper;

    private ShowPlayerSubscriber subscriber;
    private TestEventBus eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new ShowPlayerSubscriber(eventBus, playbackFeedbackHelper);
    }

    @Test
    public void showsPlayerOnSuccessfulPlaybackResult() {
        subscriber.onNext(PlaybackResult.success());

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).isTrue();
    }

    @Test
    public void showsFeedbackOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS);

        subscriber.onNext(errorResult);

        verify(playbackFeedbackHelper).showFeedbackOnPlaybackError(errorResult.getErrorReason());
    }
}
