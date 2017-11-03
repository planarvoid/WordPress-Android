package com.soundcloud.android.playback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExpandPlayerObserverTest {

    @Mock private ExpandPlayerCommand expandPlayerCommand;

    private ExpandPlayerObserver observer;

    @Before
    public void setUp() throws Exception {
        observer = new ExpandPlayerObserver(expandPlayerCommand);
    }

    @Test
    public void callsCommandOnNext() {
        PlaybackResult playbackResult = PlaybackResult.success();
        observer.onNext(playbackResult);
        verify(expandPlayerCommand).call(playbackResult);
    }

    @Test
    public void ignoresErrors() {
        observer.onError(new Throwable());
        verify(expandPlayerCommand, never()).call(any());
    }

}
