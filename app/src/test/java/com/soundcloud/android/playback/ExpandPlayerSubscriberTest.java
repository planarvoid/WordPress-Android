package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
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
        expect(eventbus.lastEventOn(EventQueue.PLAYER_UI).getKind()).toBe(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void subscriberShowAToastOnUnskippableError() {
        subscriber.onError(new PlaybackOperations.UnSkippablePeriodException());

        verify(toastViewController).showUnkippableAdToast();
    }
}