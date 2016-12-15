package com.soundcloud.android.playback;


import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.cast.DefaultCastOperations;
import com.soundcloud.android.cast.LegacyCastOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.schedulers.TimeInterval;
import rx.subjects.PublishSubject;

import javax.inject.Provider;

@RunWith(MockitoJUnitRunner.class)
public class ProgressReporterTest {

    private static final TimeInterval<Long> TIME_INTERVAL = new TimeInterval<>(3L, 2L);

    private ProgressReporter progressReporter;

    @Mock private ProgressReporter.ProgressPuller progressPuller;
    @Mock private Provider<LegacyCastOperations> legacyCastOperationsProvider;
    @Mock private Provider<DefaultCastOperations> castOperationsProvider;
    @Mock private FeatureFlags featureFlags;

    @Mock private DefaultCastOperations castOperations;

    private PublishSubject<TimeInterval<Long>> subject;

    @Before
    public void setUp() throws Exception {
        when(featureFlags.isEnabled(Flag.CAST_V3)).thenReturn(true);
        when(castOperationsProvider.get()).thenReturn(castOperations);
        progressReporter = new ProgressReporter(castOperationsProvider, legacyCastOperationsProvider, featureFlags, Schedulers.immediate());
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
