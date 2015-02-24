package com.soundcloud.android.playback;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.ui.view.AdToastViewController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class ShowPlayerSubscriberTest {
    private ShowPlayerSubscriber subscriber;

    private TestEventBus eventBus;
    @Mock private AdToastViewController toastViewController;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new ShowPlayerSubscriber(eventBus, toastViewController);
    }

    @Test
    public void subscriberExpandPlayerOnComplete() {
        subscriber.onCompleted();

        expect(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isShow()).toBeTrue();
    }

    @Test
    public void subscriberShowAToastOnUnskippableError() {
        subscriber.onError(new PlaybackOperations.UnskippablePeriodException());

        verify(toastViewController).showUnskippableAdToast();
    }
}