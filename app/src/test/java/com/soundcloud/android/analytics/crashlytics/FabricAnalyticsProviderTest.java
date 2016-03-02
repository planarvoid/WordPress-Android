package com.soundcloud.android.analytics.crashlytics;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.ForceUpdateEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.reporting.DatabaseReporting;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.reporting.Metric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;

import java.util.concurrent.Executor;

@RunWith(MockitoJUnitRunner.class)
public class FabricAnalyticsProviderTest {

    @Mock private Context context;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private FabricProvider fabricProvider;
    @Mock private DatabaseReporting databaseReporting;
    @Mock private FabricReporter fabricReporter;

    private FabricAnalyticsProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new FabricAnalyticsProvider(applicationProperties, fabricProvider, databaseReporting, fabricReporter);
        when(databaseReporting.countTracks()).thenReturn(3);
        when(fabricProvider.getExecutor()).thenReturn(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        });
    }

    @Test
    public void shouldDelayOnAppCreatedEventToFirstFlushBecauseFabricMightNotBeInitialized() {
        provider.onAppCreated(context);
        provider.flush();
        verifyZeroInteractions(fabricReporter); // fabric not yet initialized

        when(fabricProvider.isInitialized()).thenReturn(true); // unlock reports
        provider.flush();

        verify(fabricReporter).post(Metric.create("DB:RecordCount", DataPoint.numeric("tracks", 3)));
    }

    @Test
    public void shouldNotProcessOnAppCreatedMoreThanOnce() {
        when(fabricProvider.isInitialized()).thenReturn(true); // unlock reports
        provider.onAppCreated(context);

        // should only report the event once
        provider.flush();
        provider.flush();

        verify(fabricReporter).post(Metric.create("DB:RecordCount", DataPoint.numeric("tracks", 3)));
        verifyNoMoreInteractions(fabricReporter);
    }

    @Test
    public void shouldHandleForceUpdateEvents() {
        when(fabricProvider.isInitialized()).thenReturn(true); // unlock reports
        ForceUpdateEvent event = new ForceUpdateEvent("6.0", "2016.01.01-release", 423);

        provider.handleForceUpdateEvent(event);

        verify(fabricReporter).post(event.toMetric());
    }
}
