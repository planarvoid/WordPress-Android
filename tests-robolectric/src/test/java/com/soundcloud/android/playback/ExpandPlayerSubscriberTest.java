package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class ExpandPlayerSubscriberTest {
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

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        expect(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).toBeTrue();
    }

    @Test
    public void emitsOpenPlayerOnPlaybackResultSuccess() {
        subscriber.onNext(PlaybackResult.success());

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen(UIEvent.METHOD_TRACK_PLAY);
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

    @Test
    public void showsToastOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.UNSKIPPABLE);

        subscriber.onNext(errorResult);

        verify(playbackToastHelper).showToastOnPlaybackError(errorResult.getErrorReason());
    }

}
