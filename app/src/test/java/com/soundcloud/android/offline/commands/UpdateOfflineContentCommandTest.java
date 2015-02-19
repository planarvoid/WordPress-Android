package com.soundcloud.android.offline.commands;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class UpdateOfflineContentCommandTest extends StorageIntegrationTest {

    private final Urn TRACK_URN1 = Urn.forTrack(123L);
    private final Urn TRACK_URN2 = Urn.forTrack(124L);
    private final List<Urn> TRACK_URNS = Arrays.asList(TRACK_URN1, TRACK_URN2);

    private UpdateOfflineContentCommand command;

    @Before
    public void setUp() {
        command = new UpdateOfflineContentCommand(propeller());
    }

    @Test
    public void storeNewDownloadRequestsSavesRequestedDownloads() throws PropellerWriteException {
        command.with(TRACK_URNS).call();

        databaseAssertions().assertDownloadRequestsInserted(TRACK_URNS);
    }

    @Test
    public void storeNewDownloadRequestsDoesNotOverrideExistingRecords() throws PropellerWriteException {
        final long timestamp = 100;
        testFixtures().insertRequestedTrackDownload(TRACK_URN1, timestamp);

        command.with(TRACK_URNS).call();

        databaseAssertions().assertExistingDownloadRequest(timestamp, TRACK_URN1);
        databaseAssertions().assertDownloadRequestsInserted(Arrays.asList(TRACK_URN2));
    }

    @Test
    public void updatesToPendingRemovalWhenNotInLikesAnymore() throws PropellerWriteException {
        testFixtures().insertRequestedTrackDownload(TRACK_URN1, 100);

        command.with(Collections.<Urn>emptyList()).call();

        assertDownloadPendingRemoval(TRACK_URN1);
    }

    @Test
    public void updatesToPendingRemovalWhenNoLikesProvided() throws PropellerWriteException {
        testFixtures().insertRequestedTrackDownload(TRACK_URN1, 100);

        command.call();

        assertDownloadPendingRemoval(TRACK_URN1);
    }

    @Test
    public void doesNotUpdateToPendingRemovalWhenColumnIsAlreadySet() throws PropellerWriteException {
        final long removalDate = 1234567L;
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN1, removalDate);

        command.with(Collections.<Urn>emptyList()).call();

        assertDownloadPendingRemoval(TRACK_URN1, removalDate);
    }

    private void assertDownloadPendingRemoval(Urn trackUrn, long removalDate) {
        assertThat(select(from(Table.TrackDownloads.name())
                .whereEq(TableColumns.TrackDownloads._ID, trackUrn.getNumericId())
                .whereEq(TableColumns.TrackDownloads.REMOVED_AT, removalDate)), counts(1));
    }

    private void assertDownloadPendingRemoval(Urn trackUrn) {
        assertThat(select(from(Table.TrackDownloads.name())
                .whereEq(TableColumns.TrackDownloads._ID, trackUrn.getNumericId())
                .whereNotNull(TableColumns.TrackDownloads.REMOVED_AT)), counts(1));
    }
}