package com.soundcloud.android.playback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ExpandPlayerSubscriberTest extends AndroidUnitTest {
    private ExpandPlayerSubscriber subscriber;

    @Mock private ExpandPlayerCommand expandPlayerCommand;

    @Before
    public void setUp() throws Exception {
        subscriber = new ExpandPlayerSubscriber(expandPlayerCommand);
    }

    @Test
    public void callsCommandOnNext() {
        PlaybackResult playbackResult = PlaybackResult.success();
        subscriber.onNext(playbackResult);
        verify(expandPlayerCommand).call(playbackResult);
    }

    @Test
    public void ignoresErrors() {
        subscriber.onError(new Throwable());
        verify(expandPlayerCommand, never()).call(any());
    }
}
