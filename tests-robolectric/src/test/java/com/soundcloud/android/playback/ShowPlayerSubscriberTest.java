package com.soundcloud.android.playback;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS;
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
        subscriber = new ShowPlayerSubscriber(eventBus, playbackToastHelper);
    }

    @Test
    public void showsPlayerOnSuccessfulPlaybackResult() {
        subscriber.onNext(PlaybackResult.success());

        expect(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isShow()).toBeTrue();
    }

    @Test
    public void showsToastOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS);

        subscriber.onNext(errorResult);

        verify(playbackToastHelper).showToastOnPlaybackError(errorResult.getErrorReason());
    }

}