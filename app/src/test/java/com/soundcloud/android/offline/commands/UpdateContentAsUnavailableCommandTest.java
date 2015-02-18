package com.soundcloud.android.offline.commands;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UpdateContentAsUnavailableCommandTest extends StorageIntegrationTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    private UpdateContentAsUnavailableCommand command;

    @Before
    public void setUp() {
        command = new UpdateContentAsUnavailableCommand(propeller());
    }

    @Test
    public void markTrackAsUnavailable() throws Exception {
        testFixtures().insertRequestedTrackDownload(TRACK_URN, 100L);

        command.with(TRACK_URN).call();

        assertContentUnavailable(TRACK_URN);
    }

    private void assertContentUnavailable(Urn trackUrn) {
        assertThat(select(from(Table.TrackDownloads.name())
                .whereEq(TableColumns.TrackDownloads._ID, trackUrn.getNumericId())
                .whereNotNull(TableColumns.TrackDownloads.UNAVAILABLE_AT)), counts(1));
    }
}