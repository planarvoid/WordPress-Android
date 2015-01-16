package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.util.concurrent.Callable;

@RunWith(SoundCloudTestRunner.class)
public class DefaultSyncJobTest {
    
    private DefaultSyncJob defaultSyncJob;
    
    @Mock private Callable<Boolean> syncer;

    @Before
    public void setUp() throws Exception {
        defaultSyncJob = new DefaultSyncJob(syncer);
    }

    @Test
    public void wasChangedReportsTrueAfterExecutingAChangingSync() throws Exception {
        when(syncer.call()).thenReturn(true);

        defaultSyncJob.run();
        expect(defaultSyncJob.resultedInAChange()).toBeTrue();
    }

    @Test
    public void getExceptionReturnsExceptionFromSync() throws Exception {
        IOException ioException = new IOException();
        when(syncer.call()).thenThrow(ioException);

        defaultSyncJob.run();

        expect(defaultSyncJob.getSyncException()).toBe(ioException);
    }
}