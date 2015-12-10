package com.soundcloud.android.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class DatabaseReportingTest extends StorageIntegrationTest {

    private DatabaseReporting databaseReporting;

    @Before
    public void setUp() throws Exception {
        databaseReporting = new DatabaseReporting(propeller());
    }

    @Test
    public void shouldReportRecordCountMetricToBackend() {
        testFixtures().insertTrack(); // should count
        testFixtures().insertPlaylist(); // should not count

        assertThat(databaseReporting.countTracks()).isEqualTo(1);
    }
}
