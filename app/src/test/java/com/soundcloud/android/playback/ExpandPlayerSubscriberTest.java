package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.ui.view.PlaybackToastViewController;
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

    private TestEventBus eventbus;
    @Mock private PlaybackToastViewController toastViewController;

    @Before
    public void setUp() throws Exception {
        eventbus = new TestEventBus();
        subscriber = new ExpandPlayerSubscriber(eventbus, toastViewController);
    }

    @Test
    public void subscriberExpandPlayerOnComplete() {
        subscriber.onCompleted();

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        expect(eventbus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).toBeTrue();
    }

    @Test
    public void subscriberShowAToastOnUnskippableError() {
        subscriber.onError(new PlaybackOperations.UnSkippablePeriodException());

        verify(toastViewController).showUnskippableAdToast();
    }

    @Test
    public void subscriberEmitsOpenPlayerFromTrackPlay() {
        subscriber.onCompleted();

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        UIEvent event = (UIEvent) eventbus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen(UIEvent.METHOD_TRACK_PLAY);
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }
}