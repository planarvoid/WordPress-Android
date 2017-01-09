package com.soundcloud.android.analytics.crashlytics;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.ForceUpdateEvent;
import com.soundcloud.android.events.DatabaseMigrationEvent;
import com.soundcloud.android.events.FileAccessEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.reporting.DatabaseReporting;
import com.soundcloud.java.optional.Optional;
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
        provider = new FabricAnalyticsProvider(applicationProperties,
                                               fabricProvider,
                                               databaseReporting,
                                               fabricReporter);
        when(databaseReporting.countTracks()).thenReturn(3);
        when(fabricProvider.getExecutor()).thenReturn(command -> command.run());
        when(databaseReporting.pullDatabaseMigrationEvent()).thenReturn(Optional.<DatabaseMigrationEvent>absent());
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

    @Test
    public void shouldPostDBMigrationReportWithSuccessfulMigrationEvent() {
        final DatabaseMigrationEvent event = DatabaseMigrationEvent.forSuccessfulMigration(3L);

        when(fabricProvider.isInitialized()).thenReturn(true); // unlock reports
        when(databaseReporting.pullDatabaseMigrationEvent()).thenReturn(Optional.of(event));
        provider.onAppCreated(context);

        provider.flush();

        verify(fabricReporter).post(Metric.create("DBMigrationsReport",
                                                  DataPoint.string("MigrationStatus", "Success"),
                                                  DataPoint.numeric("SuccessDuration", 3L)));

    }

    @Test
    public void shouldPostDBMigrationReportWithFailedMigrationEvent() {
        final DatabaseMigrationEvent event = DatabaseMigrationEvent.forFailedMigration(1, 2, 3L, "Some SQL Exception");

        when(fabricProvider.isInitialized()).thenReturn(true); // unlock reports
        when(databaseReporting.pullDatabaseMigrationEvent()).thenReturn(Optional.of(event));
        provider.onAppCreated(context);

        provider.flush();

        verify(fabricReporter).post(Metric.create("DBMigrationsReport",
                                                  DataPoint.string("FailReason", "Some SQL Exception"),
                                                  DataPoint.string("FailVersions", "1 to 2"),
                                                  DataPoint.string("MigrationStatus", "Failed"),
                                                  DataPoint.numeric("FailDuration", 3L)));
    }

    @Test
    public void shouldPostFileAccessEvent() {
        final FileAccessEvent fileAccessEvent = new FileAccessEvent(true, true, false);
        when(fabricProvider.isInitialized()).thenReturn(true);

        provider.handleTrackingEvent(fileAccessEvent);

        verify(fabricReporter).post(Metric.create("FileAccess",
                                                  DataPoint.string("FileExists", "true"),
                                                  DataPoint.string("CanWrite", "true"),
                                                  DataPoint.string("CanRead", "false")));
        verify(fabricProvider, never()).getCrashlyticsCore();
    }
}
