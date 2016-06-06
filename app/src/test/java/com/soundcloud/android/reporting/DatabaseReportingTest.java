package com.soundcloud.android.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.DatabaseMigrationEvent;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DatabaseReportingTest extends StorageIntegrationTest {

    private DatabaseReporting databaseReporting;

    @Mock private DatabaseManager dbManager;
    @Mock private DatabaseMigrationEvent migrationEvent;

    @Before
    public void setUp() throws Exception {
        databaseReporting = new DatabaseReporting(propeller(), dbManager);
    }

    @Test
    public void shouldReportRecordCountMetricToBackend() {
        testFixtures().insertTrack(); // should count
        testFixtures().insertPlaylist(); // should not count

        assertThat(databaseReporting.countTracks()).isEqualTo(1);
    }

    @Test
    public void shouldPullExistingDBReport() {
        when(dbManager.pullMigrationReport()).thenReturn(migrationEvent);
        assertThat(databaseReporting.pullDatabaseMigrationEvent().isPresent()).isTrue();
        assertThat(databaseReporting.pullDatabaseMigrationEvent().get()).isEqualTo(migrationEvent);
    }

    @Test
    public void shouldReturnOptionalAbsentWhenNoDBReport() {
        when(dbManager.pullMigrationReport()).thenReturn(null);
        assertThat(databaseReporting.pullDatabaseMigrationEvent().isPresent()).isFalse();
    }

}
