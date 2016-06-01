package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

public class ExpandPlayerSubscriberTest extends AndroidUnitTest {
    private ExpandPlayerSubscriber subscriber;

    private TestEventBus eventBus;
    @Mock private PlaybackToastHelper playbackToastHelper;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new ExpandPlayerSubscriber(eventBus, playbackToastHelper);
    }

    @Test
    public void expandsPlayerOnPlaybackResultSuccess() {
        subscriber.onNext(PlaybackResult.success());

        Robolectric.flushForegroundThreadScheduler();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).isTrue();
    }

    @Test
    public void emitsOpenPlayerOnPlaybackResultSuccess() {
        subscriber.onNext(PlaybackResult.success());

        Robolectric.flushForegroundThreadScheduler();
        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen();
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void showsToastOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.UNSKIPPABLE);

        subscriber.onNext(errorResult);

        verify(playbackToastHelper).showToastOnPlaybackError(errorResult.getErrorReason());
    }

}
