package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlayerListenerTest {

    @Mock
    private PlaybackOperations playbackOperations;

    private PlayerListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new PlayerListener(playbackOperations);
    }

    @Test
    public void trackChangeOnPlayerListenerSetsPlayQueuePosition() {
        listener.onTrackChanged(3);
        verify(playbackOperations).setPlayQueuePosition(3);
    }

}