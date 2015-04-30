package com.soundcloud.android.playback;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
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
    @Mock private PlaybackToastHelper playbackToastHelper;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new ShowPlayerSubscriber(playbackToastHelper);
    }

    @Test
    public void subscriberExpandPlayerOnComplete() {
        subscriber.onCompleted();

        expect(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isShow()).toBeTrue();
    }

    @Test
    public void onErrorPassesExceptionToPlaybackToastHelper() {
        final Exception someException = new Exception("some exception");

        subscriber.onError(someException);

        verify(playbackToastHelper).showToastOnPlaybackError(someException);
    }
}