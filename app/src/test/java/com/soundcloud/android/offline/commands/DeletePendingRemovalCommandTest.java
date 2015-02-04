package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.test.matchers.RowCountMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class DeletePendingRemovalCommandTest extends StorageIntegrationTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private SecureFileStorage fileStorage;
    private DeletePendingRemovalCommand command;

    @Before
    public void setUp() {
        command = new DeletePendingRemovalCommand(fileStorage, propeller());
    }

    @Test
    public void deletePendingRemovalTrackFromTheDatabase() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN, 100L);

        command.call();

        assertDownloadPendingRemovalCount(TRACK_URN, counts(0));
        verify(fileStorage).deleteTrack(TRACK_URN);
    }

    @Test
    public void returningRemovedTracks() throws Exception {
        final long removedAtTimestamp = System.currentTimeMillis() - 5 * 60000;
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN, removedAtTimestamp);

        List<Urn> removals = command.call();
        expect(removals).toContainExactly(TRACK_URN);
    }

    @Test
    public void doesNotDeleteNotPendingRemoval() throws Exception {
        testFixtures().insertCompletedTrackDownload(TRACK_URN, System.currentTimeMillis());

        expect(command.call()).toBeEmpty();
        assertDownloadedTrackCount(TRACK_URN, counts(1));
        verify(fileStorage, never()).deleteTrack(TRACK_URN);
    }

    public void assertDownloadPendingRemovalCount(Urn trackUrn, RowCountMatcher count) {
        assertThat(select(from(Table.TrackDownloads.name())
                .whereEq(TableColumns.TrackDownloads._ID, trackUrn.getNumericId())
                .whereNotNull(TableColumns.TrackDownloads.REMOVED_AT)), count);
    }

    public void assertDownloadedTrackCount(Urn trackUrn, RowCountMatcher count) {
        assertThat(select(from(Table.TrackDownloads.name())
                .whereEq(TableColumns.TrackDownloads._ID, trackUrn.getNumericId())
                .whereNull(TableColumns.TrackDownloads.REMOVED_AT)), count);
    }
}