package com.soundcloud.android.playback;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class ProgressReporterTest {

    private ProgressReporter progressReporter;

    @Mock private ProgressReporter.ProgressPusher progressPusher;

    @Before
    public void setUp() throws Exception {
        progressReporter = new ProgressReporter();
        progressReporter.setProgressPusher(progressPusher);
    }

    @Test
    public void startReportsProgressToListener() throws Exception {
        progressReporter.start();

        verify(progressPusher).pushProgress();
    }

    @Test
    public void startQueuesAnotherProgressReport() throws Exception {
        progressReporter.start();

        Robolectric.runUiThreadTasksIncludingDelayedTasks();

        verify(progressPusher, times(2)).pushProgress();
    }

    @Test
    public void stopStopsReporting() throws Exception {
        progressReporter.start();
        progressReporter.stop();

        Robolectric.runUiThreadTasksIncludingDelayedTasks();

        verify(progressPusher).pushProgress(); // only the first one
    }
}