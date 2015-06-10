package com.soundcloud.android.playback;


import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.cast.CastOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.schedulers.TimeInterval;
import rx.subjects.PublishSubject;

@RunWith(SoundCloudTestRunner.class)
public class ProgressReporterTest {

    public static final TimeInterval<Long> TIME_INTERVAL = new TimeInterval<>(3L, 2L);

    private ProgressReporter progressReporter;

    @Mock private ProgressReporter.ProgressPuller progressPuller;
    @Mock private CastOperations castOperations;

    private PublishSubject<TimeInterval<Long>> subject;

    @Before
    public void setUp() throws Exception {
        progressReporter = new ProgressReporter(castOperations);
        progressReporter.setProgressPuller(progressPuller);
        subject = PublishSubject.create();
        when(castOperations.intervalForProgressPull()).thenReturn(subject);
    }

    @Test
    public void startReportsProgressToListener() {
        progressReporter.start();

        subject.onNext(TIME_INTERVAL);

        verify(progressPuller).pullProgress();
    }

    @Test
    public void startReportsMultipleProgressEventsToListener() {
        progressReporter.start();

        subject.onNext(TIME_INTERVAL);
        subject.onNext(TIME_INTERVAL);

        verify(progressPuller, times(2)).pullProgress();
    }

    @Test
    public void stopStopsReporting() {
        progressReporter.start();
        progressReporter.stop();

        subject.onNext(TIME_INTERVAL);

        verify(progressPuller, never()).pullProgress();
    }
}