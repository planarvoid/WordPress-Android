package com.soundcloud.android.sync.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.concurrent.Callable;

@RunWith(MockitoJUnitRunner.class)
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
        assertThat(defaultSyncJob.resultedInAChange()).isTrue();
    }

    @Test
    public void getExceptionReturnsExceptionFromSync() throws Exception {
        IOException ioException = new IOException();
        when(syncer.call()).thenThrow(ioException);

        defaultSyncJob.run();

        assertThat(defaultSyncJob.getException()).isSameAs(ioException);
    }

    @Test
    public void wasSuccessIsFalseAfterException() throws Exception {
        IOException ioException = new IOException();
        when(syncer.call()).thenThrow(ioException);

        defaultSyncJob.run();

        assertThat(defaultSyncJob.wasSuccess()).isFalse();
    }
}
