package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class TrackPagerAdapterTest {

    @Mock
    private PlayQueueManager playQueueManager;

    private TrackPagerAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new TrackPagerAdapter(playQueueManager);
    }

    @Test
    public void getCountReturnsCurrentPlayQueueSize() {
        when(playQueueManager.getCurrentPlayQueueSize()).thenReturn(10);
        expect(adapter.getCount()).toBe(10);
    }

}